package com.slam.concertreservation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = {
        Hhp7ConcertReservationApplication.class,
        MetricsAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class
})
class Hhp7ConcertReservationApplicationTests {

    @Test
    void contextLoads() {
    }

}
