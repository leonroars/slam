# P99 Latency Spike Diagnosis and Resolution

> Root cause analysis of a P99 latency spike from 48ms to 1.04s under load,
> traced to memory pressure causing GC-induced connection pool starvation.

**Result:** P99 1,040ms → 355ms (-66%) | Throughput 813 → 1,250 RPS (+54%)

---

## 1. Problem

During K6 load testing at ~800 RPS, tail latencies spiked abruptly between two 15-second observation windows:

| Metric | 798 RPS (t₁) | 813 RPS (t₂) | Change |
|--------|:------------:|:------------:|:------:|
| P99 Latency | 48.5ms | 1.04s | +2,044% |
| P90 Latency | 13.6ms | 543ms | +3,892% |
| P50 Latency | 3.1ms | 18.3ms | Stable |
| HikariCP Pending Threads | 0 | 189 | New |
| HikariCP Max Usage Time | 480ms | 480ms | **No change** |
| Tomcat Busy Threads | 5 | 200 | +3,900% |
| GC Pause Sum (1 min) | 308ms | 1.25s | +305% |
| GC Count (1 min) | 70 | 80 | +14% |

Key observation: a **+1.87% increase in RPS** triggered a +305% increase in GC pause time,
while HikariCP Max Usage Time remained flat.
This ruled out an increase in per-query DB cost and pointed toward something blocking connection *returns*, not connection *usage*.

---

## 2. Investigation

Four hypotheses were ranked by explanatory power against the observed data:

| Hypothesis | Key Evidence | Verdict |
|-----------|-------------|---------|
| **A. Memory pressure → excessive GC** | GC count +14% but pause sum +305% → individual pauses grew longer | **Primary suspect** |
| B. Connection pool undersized | Little's Law: 1K RPS × 4.32ms = 4.34 connections needed; pool size = 10. Usage time flat, only acquire time spiked. | Ruled out — pool had capacity; acquire delay indicates return blockage, not shortage |
| C. DB bottleneck | Max and avg usage time unchanged | Ruled out — DB work itself was not slower |
| D. CPU saturation | System CPU hit 90% | Contributing factor, not root cause — 88% of CPU increase was within JVM process; resolved after heap fix |

Hypothesis A was investigated first via JFR profiling.

---

## 3. Root Cause

### JFR: Memory Pressure Confirmed

```
Heap Used:      202 MB
Heap Committed: 256 MB
Free Ratio:     (256 - 202) / 256 = 21%

G1GC default: MinHeapFreeRatio = 40%
21% < 40% → JVM decided to expand heap

Allocation Total (1 min window at spike): 8.28 GiB (~138 MB/s)
```

JFR recorded heap committed jumping from 256MB to 339MB at 01:14:08,
confirming the JVM itself recognized memory pressure and attempted expansion.
The allocation rate of ~138MB/s against a 256MB committed heap meant
Eden space was being exhausted multiple times per second,
driving the observed GC frequency.

### Causal Chain

```
Memory pressure (79% utilization, free ratio 21%)
    │
    ▼
Frequent + longer GC pauses (308ms → 1.25s/min, individual pauses ~31ms)
    │
    ▼
Threads suspended during STW pauses, connections held unreturned
    │
    ├─ Per GC pause: 800 RPS × 31ms = ~25 requests arriving
    │  Pool size = 10 → 15 requests must wait per pause
    │
    ▼
HikariCP pending queue grows (0 → 189 threads)
    │
    ▼
Acquire time spikes, P90/P99 latency spikes
```

### Cross-Validation: Thread Dump vs Grafana

| Time | Source | Threads Waiting on Connection |
|------|--------|:---:|
| 01:14:22 | JFR Thread Dump | **187** |
| 01:14:30 | Grafana HikariCP Pending | **189** |

Two independent data sources, 8 seconds apart, reporting the same phenomenon at the same scale.

### Why P50 Stayed Stable

STW pauses occupied ~2.1% of wall-clock time (1.25s per 60s),
but the connection pool bottleneck (25 arrivals vs 10 slots per pause)
amplified the impact for requests caught during or immediately after a pause.
Requests arriving between pauses were processed normally, keeping P50 low.

### Memory Leak Ruled Out

- Heap post-GC slope remained constant (21MB per 5s, no acceleration)
- Old gen promotion volume near zero
- No recurrence after heap resize

---

## 4. Resolution

With memory pressure confirmed as the root cause, two paths were available:

**Option A — Reduce allocation rate** to lower GC frequency at the same heap size.
**Option B — Increase heap capacity** so the existing allocation rate produces less GC pressure.

### Allocation Profile Assessment

JFR recorded ~138 MB/s allocation during the spike window (8.28 GiB / 60s).
At ~800 RPS, this translates to **~172 KB per request** — within the normal range for a Spring Boot read API including framework overhead.

The top allocator by class was `byte[]`. `ObjectAllocationOutsideTLAB` stack traces for `byte[]` showed the following size distribution and origins:

<!-- JFR OutsideTLAB byte[] size distribution screenshot here -->

| Size | Allocator | Stack Trace Root |
|------|-----------|-----------------|
| 8 KB | Tomcat NIO socket buffer | `SocketBufferHandler.<init>` → `NioEndpoint.setSocketOptions` |
| 16 KB | Prometheus cgroup reader, Lettuce NIO, HikariPool, JFR tasks | `BufferedReader.<init>` → `CgroupV2Subsystem`, Netty I/O, pool management |
| 115 KB | Prometheus metrics serialization | `ByteArrayOutputStream.<init>` → `PrometheusScrapeEndpoint.scrape` |

Every identified allocator was an infrastructure or monitoring component. No application-level code appeared in the top allocation paths.

> Note: `ObjectAllocationInNewTLAB` was not enabled in the recording profile (`default.jfc`), so this analysis covers only allocations that exceeded TLAB capacity. The full allocation profile — including normal-sized objects allocated within TLABs — was not captured.

### Decision: Option B

The per-request allocation rate was not abnormal, and the largest allocators were infrastructure components outside application control. Code-level optimization would not have materially reduced GC pressure.

Initial configuration: `-Xms256m -Xmx512m`.
Changed to: `-Xms2g -Xmx2g`.

- Large increase to clearly confirm memory pressure as root cause
- Xms pinned to Xmx to eliminate runtime heap resizing overhead

### Result

| Metric | Before | After |
|--------|:------:|:-----:|
| P99 Latency | 1,040ms | 355ms |
| HikariCP Pending | 189 | 0 |
| HikariCP Active (max) | — | 4 |
| GC Count (1 min) | 80 | 4 |
| Tomcat Busy Threads | 200 | 3 |

---

## 5. Validation

### Little's Law Cross-Check

```
Predicted: 1K RPS × 4.32ms = 4.34 connections needed
Measured after fix: max 4 active connections
```

Before the fix, GC pauses inflated effective connection hold time,
masking the true resource requirement.
After removing GC pressure, measured utilization matched the theoretical prediction —
confirming that the original pool size was adequate
and the bottleneck was purely GC-induced hold time inflation.

### Key Takeaways

1. **Aggregate metrics can mislead.** Grafana showed correlation but could not establish causation direction. JFR profiling was necessary to confirm whether GC caused the stall or the stall caused GC.

2. **Cross-validation builds confidence.** Thread dump (187) and Grafana pending (189) independently confirmed the same phenomenon, ruling out measurement error.

3. **Quantitative reasoning narrows the search space.** Little's Law eliminated connection pool sizing as a hypothesis before any profiling began, saving investigation time.

### Limitations

The fix changed two variables simultaneously — heap capacity (512MB → 2GB) and dynamic resizing (Xms ≠ Xmx → Xms = Xmx).

During post-fix review, it became apparent that the metric spike had coincided with heap committed expansion (256MB → 339MB),
 and the GCLocker Initiated GC at 01:14:07 — immediately before the spike — carried a notably longer pause than surrounding cycles.

This raised the possibility that dynamic resizing itself was an independent contributing factor, and that pinning Xms=Xmx=512MB without raising the ceiling might have been sufficient.

By the time this gap was identified, the test environment had changed, making controlled re-testing impractical.