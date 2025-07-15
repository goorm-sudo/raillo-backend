package com.sudo.railo.global.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락 어노테이션
 * 
 * AOP를 통해 메서드 레벨에서 분산 락을 적용
 * 주로 동시성 제어가 필요한 비즈니스 로직에 사용
 * 
 * 사용 예시:
 * @DistributedLock(key = "#paymentId", prefix = "payment")
 * public void processPayment(String paymentId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    
    /**
     * 락 키 (SpEL 표현식 지원)
     * 예: "#paymentId", "#member.id", "#request.calculationId"
     */
    String key();
    
    /**
     * 락 키 접두사 (도메인별 구분)
     * 예: "payment", "mileage", "reservation"
     */
    String prefix() default "";
    
    /**
     * 락 만료 시간 (기본: 30초)
     */
    long expireTime() default 30;
    
    /**
     * 락 대기 시간 (기본: 5초)
     */
    long waitTime() default 5;
    
    /**
     * 시간 단위 (기본: 초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 락 획득 실패 시 예외 발생 여부 (기본: true)
     * false인 경우 null 반환
     */
    boolean throwExceptionOnFailure() default true;
}