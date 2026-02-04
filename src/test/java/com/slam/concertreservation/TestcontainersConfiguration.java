package com.slam.concertreservation;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
class TestcontainersConfiguration {

    public static final MySQLContainer<?> MYSQL_CONTAINER;
    public static final GenericContainer<?> REDIS_CONTAINER;

    static {
        // CI 환경이면 Testcontainers 스킵
        if (isCI()) {
            MYSQL_CONTAINER = null;
            REDIS_CONTAINER = null;
        } else {
            // 1) MySQL Container
            MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("mydatabase")
                    .withUsername("myuser")
                    .withPassword("secret");
            MYSQL_CONTAINER.start();

            System.setProperty("spring.datasource.url",
                    MYSQL_CONTAINER.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
            System.setProperty("spring.datasource.username", MYSQL_CONTAINER.getUsername());
            System.setProperty("spring.datasource.password", MYSQL_CONTAINER.getPassword());

            // 2) Redis Container
            REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("bitnami/redis:7.4"))
                    .withExposedPorts(6379)
                    .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                    .withEnv("REDIS_DISABLE_COMMANDS", "FLUSHDB,FLUSHALL");
            REDIS_CONTAINER.start();

            System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
            System.setProperty("spring.data.redis.port", REDIS_CONTAINER.getFirstMappedPort().toString());
        }
    }

    private static boolean isCI() {
        return "true".equals(System.getenv("CI"));
    }

    @PreDestroy
    public void preDestroy() {
        if (MYSQL_CONTAINER != null && MYSQL_CONTAINER.isRunning()) {
            MYSQL_CONTAINER.stop();
        }
        if (REDIS_CONTAINER != null && REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.stop();
        }
    }
}