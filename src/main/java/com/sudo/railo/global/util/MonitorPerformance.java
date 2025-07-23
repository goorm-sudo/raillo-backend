package com.sudo.railo.global.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {

	String value() default ""; // 메서드 설명

	boolean enableN1Detection() default true; // N+1 감지 여부
}
