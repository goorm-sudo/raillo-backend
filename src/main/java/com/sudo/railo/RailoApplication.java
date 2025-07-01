package com.sudo.railo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync  // 이벤트 처리용
@EnableJpaAuditing

// JPA Repository 패키지 명시적 지정 (Redis 패키지 제외)
@EnableJpaRepositories(basePackages = {
    "com.sudo.railo.payment.infrastructure.persistence",
    "com.sudo.railo.booking.infra",
    "com.sudo.railo.member.infra",
    "com.sudo.railo.train.infrastructure"
})

// Redis Repository 패키지 별도 지정 (JPA와 분리)
@EnableRedisRepositories(basePackages = {
    "com.sudo.railo.global.redis"
})
public class RailoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RailoApplication.class, args);
	}

}
