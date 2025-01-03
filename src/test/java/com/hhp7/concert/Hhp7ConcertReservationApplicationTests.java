package com.hhp7.concert;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class Hhp7ConcertReservationApplicationTests {

    @Test
    void contextLoads() {
    }

}
