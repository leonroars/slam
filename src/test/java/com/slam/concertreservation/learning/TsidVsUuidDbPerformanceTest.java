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
 * - 대량 삽입 시 UUID(String PK) vs TSID(Long PK) 성능 차이 측정
 * - B-Tree 인덱스 성능 특성 비교
 */
@SpringBootTest
@ActiveProfiles("test")
public class TsidVsUuidDbPerformanceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final int TOTAL_RECORDS = 10000;

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
        printResults(uuidInsertTime, tsidInsertTime, uuidSelectTime, tsidSelectTime);

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

        // 전체 조회 (ORDER BY PK)
        jdbcTemplate.queryForList("SELECT * FROM test_uuid ORDER BY id LIMIT 1000");

        // 범위 조회 시뮬레이션
        for (int i = 0; i < 100; i++) {
            jdbcTemplate.queryForList("SELECT * FROM test_uuid WHERE name LIKE 'test_1%' LIMIT 100");
        }

        return System.currentTimeMillis() - startTime;
    }

    private long measureTsidSelectTime() {
        long startTime = System.currentTimeMillis();

        // 전체 조회 (ORDER BY PK)
        jdbcTemplate.queryForList("SELECT * FROM test_tsid ORDER BY id LIMIT 1000");

        // 범위 조회 시뮬레이션
        for (int i = 0; i < 100; i++) {
            jdbcTemplate.queryForList("SELECT * FROM test_tsid WHERE name LIKE 'test_1%' LIMIT 100");
        }

        return System.currentTimeMillis() - startTime;
    }

    private void printResults(long uuidInsert, long tsidInsert, long uuidSelect, long tsidSelect) {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           UUID vs TSID 데이터베이스 성능 비교 결과                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println(
                "║  총 레코드 수: " + String.format("%,d", TOTAL_RECORDS) + "                                            ║");
        System.out.println(
                "║  배치 크기: " + String.format("%,d", BATCH_SIZE) + "                                                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  메트릭              │  UUID (String)  │  TSID (Long)  │ 차이    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  삽입 시간 (ms)      │  %,13d  │  %,11d  │ %+.1f%%   ║%n",
                uuidInsert, tsidInsert, ((double) (tsidInsert - uuidInsert) / uuidInsert) * 100);
        System.out.printf("║  조회 시간 (ms)      │  %,13d  │  %,11d  │ %+.1f%%   ║%n",
                uuidSelect, tsidSelect, ((double) (tsidSelect - uuidSelect) / uuidSelect) * 100);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║  PK 저장 크기        │  36 bytes       │  8 bytes      │ -78%    ║");
        System.out.println("║  인덱스 효율         │  낮음           │  높음         │ -       ║");
        System.out.println("║  정렬 가능성         │  불가능         │  시간순 정렬  │ -       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }

    private void dropTestTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_uuid");
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_tsid");
    }
}
