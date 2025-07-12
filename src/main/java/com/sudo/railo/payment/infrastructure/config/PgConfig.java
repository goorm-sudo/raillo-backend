package com.sudo.railo.payment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * PG 연동을 위한 설정
 */
@Configuration
public class PgConfig {
    
    /**
     * PG API 호출용 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10초
        factory.setReadTimeout(30000);    // 30초
        
        return new RestTemplate(factory);
    }
} 