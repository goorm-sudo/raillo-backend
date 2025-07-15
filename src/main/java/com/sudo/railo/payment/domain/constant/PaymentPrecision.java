package com.sudo.railo.payment.domain.constant;

/**
 * 결제 도메인 BigDecimal 정밀도 상수
 * 
 * 모든 결제 관련 엔티티에서 사용하는 BigDecimal 필드의
 * 정밀도(precision)와 소수점(scale)을 통일하여 관리
 */
public final class PaymentPrecision {
    
    private PaymentPrecision() {
        // 상수 클래스이므로 인스턴스화 방지
    }
    
    /**
     * 금액(원화) 관련 필드 정밀도
     * - 최대 999,999,999,999,999원까지 표현 가능 (999조)
     * - 소수점 2자리까지 표현 (전 처리용)
     */
    public static final int AMOUNT_PRECISION = 15;
    public static final int AMOUNT_SCALE = 2;
    
    /**
     * 마일리지 포인트 관련 필드 정밀도
     * - 최대 9,999,999,999 포인트까지 표현 가능
     * - 소수점 없음 (정수형)
     */
    public static final int MILEAGE_PRECISION = 10;
    public static final int MILEAGE_SCALE = 0;
    
    /**
     * 비율/퍼센트 관련 필드 정밀도
     * - 최대 99.999%까지 표현 가능
     * - 소수점 3자리까지 표현
     */
    public static final int RATE_PRECISION = 5;
    public static final int RATE_SCALE = 3;
    
    /**
     * 수량 관련 필드 정밀도
     * - 최대 999,999개까지 표현 가능
     * - 소수점 없음 (정수형)
     */
    public static final int QUANTITY_PRECISION = 6;
    public static final int QUANTITY_SCALE = 0;
}