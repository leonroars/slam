package com.hhp7.concertreservation;

import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
class TestcontainersConfiguration {

    public static final MySQLContainer<?> MYSQL_CONTAINER;
    public static final GenericContainer<?> REDIS_CONTAINER;
    public static final GenericContainer<?> KAFKA_CONTAINER;

    static {
        // 1) MySQL Container
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
                .withDatabaseName("mydatabase")
                .withUsername("myuser")
                .withPassword("secret");
        MYSQL_CONTAINER.start();

        // Apply MySQL connection properties
        System.setProperty("spring.datasource.url",
                MYSQL_CONTAINER.getJdbcUrl() + "?characterEncoding=UTF-8&serverTimezone=UTC");
        System.setProperty("spring.datasource.username", MYSQL_CONTAINER.getUsername());
        System.setProperty("spring.datasource.password", MYSQL_CONTAINER.getPassword());

        // 2) Redis Container
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("bitnami/redis:7.4"))
                .withExposedPorts(6379)  // default redis port
                .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                .withEnv("REDIS_DISABLE_COMMANDS", "FLUSHDB,FLUSHALL");
        REDIS_CONTAINER.start();

        String redisHost = REDIS_CONTAINER.getHost();
        Integer redisPort = REDIS_CONTAINER.getFirstMappedPort();

        System.setProperty("spring.data.redis.host", redisHost);
        System.setProperty("spring.data.redis.port", redisPort.toString());

        // 3) Kafka Container : 전진님 감사합니다. 사랑합니다.
        KAFKA_CONTAINER = new GenericContainer<>(DockerImageName.parse("bitnami/kafka:latest"))
                .withExposedPorts(9092, 9093)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_CFG_NODE_ID", "0")
                .withEnv("KAFKA_CFG_PROCESS_ROLES", "controller,broker")
                .withEnv("KAFKA_CFG_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094")
                .withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092,EXTERNAL://localhost:9094")
                .withEnv("KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP",
                        "CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .withEnv("KAFKA_CFG_CONTROLLER_QUORUM_VOTERS", "0@kafka:9093")
                .withEnv("KAFKA_CFG_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                .withEnv("KAFKA_CREATE_TOPICS", "topic1:1:1")
                .waitingFor(Wait.forLogMessage(".*Starting Kafka.*\\n", 1));

        KAFKA_CONTAINER.setPortBindings(List.of("9092:9092"));

        KAFKA_CONTAINER.start();
    }

    @PreDestroy
    public void preDestroy() {
        if (MYSQL_CONTAINER.isRunning()) {
            MYSQL_CONTAINER.stop();
        }
        if (REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.stop();
        }
        if (KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.stop();
        }
    }
}