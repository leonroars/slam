package com.slam.concertreservation.interfaces.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("UserResponse ID should be serialized as String")
    void userResponseSerialization() throws Exception {
        UserResponse dto = UserResponse.builder()
                .id("1234567890123456789")
                .name("Test User")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":\"1234567890123456789\"");
    }

    @Test
    @DisplayName("ConcertResponse ID should be serialized as String")
    void concertResponseSerialization() throws Exception {
        ConcertResponse dto = ConcertResponse.builder()
                .id("9876543210987654321")
                .name("Test Concert")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":\"9876543210987654321\"");
    }

    // Add more tests for other DTOs as needed
}
