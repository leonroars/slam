package com.hhp7.concertreservation.infrastructure.persistence.jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // Main Application 에 어노테이션을 붙이면 애플리케이션 전체 레벨에 JPA의 흔적이 남는 것처럼 느껴졌습니다.
    // 결국 이러한 작업 또한 나중에 JPA를 사용하지 않게 될 경우 불필요한 수정을 요구하기 때문입니다.
    // 따라서 Infrastructure 패키지 내에 별도로 JpaConfig 클래스를 정의하고 위와 같이 조치함으로써,
    // JPA를 활용하고 영향을 받는 범위가 적절하게 격리될 수 있도록 하였습니다.
}
