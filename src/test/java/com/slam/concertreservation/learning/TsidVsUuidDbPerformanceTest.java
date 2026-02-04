package com.slam.concertreservation.learning;

import io.hypersistence.tsid.TSID;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * UUID vs TSID 데이터베이스 삽입 성능 비교 테스트
 * 
 * 테스트 목적:
 * - 10,000건 삽입 시 UUID(String PK) vs TSID(Long PK) 성능 차이 측정
 * 
 */
@SpringBootTest(properties = "spring.datasource.hikari.auto-commit=true")
@ActiveProfiles("test")
public class TsidVsUuidDbPerformanceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final int TOTAL_RECORDS = 100000;

    @Test
    @DisplayName("UUID vs TSID 삽입 성능 비교")
    void compareInsertPerformance() {
        // 테이블 생성
        createTestTables();

        // UUID 삽입 성능 측정
        long uuidInsertTime = measureUuidInsertTime();

        // TSID 삽입 성능 측정
        long tsidInsertTime = measureTsidInsertTime();

        // 조회 성능 측정
        long uuidSelectTime = measureUuidSelectTime();
        long tsidSelectTime = measureTsidSelectTime();

        // 결과 출력
        printResults(uuidInsertTime, tsidInsertTime, uuidSelectTime, tsidSelectTime, "test_uuid", "test_tsid");

        // 테이블 삭제
        dropTestTables();
    }

    private void createTestTables() {
        jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS test_uuid (
                        id VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);

        jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS test_tsid (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
    }

    private long measureUuidInsertTime() {
        long startTime = System.currentTimeMillis();

        for (int batch = 0; batch < TOTAL_RECORDS / BATCH_SIZE; batch++) {
            StringBuilder sql = new StringBuilder("INSERT INTO test_uuid (id, name) VALUES ");
            for (int i = 0; i < BATCH_SIZE; i++) {
                if (i > 0)
                    sql.append(",");
                sql.append("('").append(UUID.randomUUID().toString()).append("', 'test_").append(batch * BATCH_SIZE + i)
                        .append("')");
            }
            jdbcTemplate.execute(sql.toString());
        }

        return System.currentTimeMillis() - startTime;
    }

    private long measureTsidInsertTime() {
        long startTime = System.currentTimeMillis();

        for (int batch = 0; batch < TOTAL_RECORDS / BATCH_SIZE; batch++) {
            StringBuilder sql = new StringBuilder("INSERT INTO test_tsid (id, name) VALUES ");
            for (int i = 0; i < BATCH_SIZE; i++) {
                if (i > 0)
                    sql.append(",");
                sql.append("(").append(TSID.fast().toLong()).append(", 'test_").append(batch * BATCH_SIZE + i)
                        .append("')");
            }
            jdbcTemplate.execute(sql.toString());
        }

        return System.currentTimeMillis() - startTime;
    }

    private long measureUuidSelectTime() {
        long startTime = System.currentTimeMillis();

        // ORDER BY PK 전체 정렬 조회 (PK 인덱스 스캔 성능 측정)
        jdbcTemplate.queryForList("SELECT * FROM test_uuid ORDER BY id");

        return System.currentTimeMillis() - startTime;
    }

    private long measureTsidSelectTime() {
        long startTime = System.currentTimeMillis();

        // ORDER BY PK 전체 정렬 조회 (PK 인덱스 스캔 성능 측정)
        jdbcTemplate.queryForList("SELECT * FROM test_tsid ORDER BY id");

        return System.currentTimeMillis() - startTime;
    }

    private void printResults(long uuidInsert, long tsidInsert, long uuidSelect, long tsidSelect, String uuidTable,
            String tsidTable) {
        // 테이블 통계 갱신 및 조회
        jdbcTemplate.execute("ANALYZE TABLE " + uuidTable);
        jdbcTemplate.execute("ANALYZE TABLE " + tsidTable);

        // 실제 데이터 개수 확인
        long uuidCount = jdbcTemplate.queryForObject("SELECT count(*) FROM " + uuidTable, Long.class);
        long tsidCount = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tsidTable, Long.class);

        long uuidDataLength = getTableMetric(uuidTable, "Data_length");
        long uuidIndexLength = getTableMetric(uuidTable, "Index_length");
        long uuidDataFree = getTableMetric(uuidTable, "Data_free");

        long tsidDataLength = getTableMetric(tsidTable, "Data_length");
        long tsidIndexLength = getTableMetric(tsidTable, "Index_length");
        long tsidDataFree = getTableMetric(tsidTable, "Data_free");

        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           UUID vs TSID 데이터베이스 성능 비교 결과                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println(
                "║  총 레코드 수: " + String.format("%,d", TOTAL_RECORDS) + "                                           ║");
        System.out.println(
                "║  배치 크기: " + String.format("%,d", BATCH_SIZE) + "                                                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  실제 저장 건수      │  %,13d  │  %,11d  │         ║%n", uuidCount, tsidCount);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  메트릭              │  UUID (String)  │  TSID (Long)  │ 차이    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  삽입 시간 (ms)      │  %,13d  │  %,11d  │ %s   ║%n",
                uuidInsert, tsidInsert, formatDiff(uuidInsert, tsidInsert));
        System.out.printf("║  조회 시간 (ms)      │  %,13d  │  %,11d  │ %s   ║%n",
                uuidSelect, tsidSelect, formatDiff(uuidSelect, tsidSelect));
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Table Size (Bytes)  │  %,13d  │  %,11d  │ %s   ║%n",
                uuidDataLength, tsidDataLength, formatDiff(uuidDataLength, tsidDataLength));
        System.out.println("║  (Clustered Index)   │                 │               │         ║");
        System.out.printf("║  Data Free (Bytes)   │  %,13d  │  %,11d  │         ║%n", uuidDataFree, tsidDataFree);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  * Table Size가 더 큰 이유: UUID의 랜덤 삽입으로 인한 Page Split   ║");
        System.out.println("║    및 내부 파편화(Internal Fragmentation) 발생 때문.               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }

    private String formatDiff(long uuidVal, long tsidVal) {
        if (uuidVal == 0)
            return "   -   ";
        double diff = ((double) (tsidVal - uuidVal) / uuidVal) * 100;
        return String.format("%+.1f%%", diff);
    }

    private long getTableMetric(String tableName, String metricColumn) {
        try {
            java.util.Map<String, Object> status = jdbcTemplate.queryForMap("SHOW TABLE STATUS LIKE ?", tableName);
            Object value = status.get(metricColumn);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return 0L;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    private void dropTestTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_uuid");
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_tsid");
    }
}
