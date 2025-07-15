package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.entity.RefundStatus;
import com.sudo.railo.payment.domain.entity.RefundType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 응답 DTO
 * 
 * @deprecated isRefundableByTime 필드명이 refundableByTime으로 변경될 예정입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponseDto {
    
    private Long refundCalculationId;
    private Long paymentId;
    private Long reservationId;
    private Long memberId;
    
    private BigDecimal originalAmount;
    private BigDecimal mileageUsed;
    private BigDecimal refundFeeRate;
    private BigDecimal refundFee;
    private BigDecimal refundAmount;
    private BigDecimal mileageRefundAmount;
    
    private LocalDateTime trainDepartureTime;
    private LocalDateTime trainArrivalTime;
    private LocalDateTime refundRequestTime;
    private LocalDateTime processedAt;
    
    private RefundType refundType;
    private RefundStatus refundStatus;
    private String refundReason;
    
    @JsonProperty("isRefundableByTime")
    private Boolean isRefundableByTime;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * RefundCalculation 엔티티를 DTO로 변환
     */
    public static RefundResponseDto from(RefundCalculation refundCalculation) {
        return RefundResponseDto.builder()
            .refundCalculationId(refundCalculation.getId())
            .paymentId(refundCalculation.getPaymentId())
            .reservationId(refundCalculation.getReservationId())
            .memberId(refundCalculation.getMemberId())
            .originalAmount(refundCalculation.getOriginalAmount())
            .mileageUsed(refundCalculation.getMileageUsed())
            .refundFeeRate(refundCalculation.getRefundFeeRate())
            .refundFee(refundCalculation.getRefundFee())
            .refundAmount(refundCalculation.getRefundAmount())
            .mileageRefundAmount(refundCalculation.getMileageRefundAmount())
            .trainDepartureTime(refundCalculation.getTrainDepartureTime())
            .trainArrivalTime(refundCalculation.getTrainArrivalTime())
            .refundRequestTime(refundCalculation.getRefundRequestTime())
            .processedAt(refundCalculation.getProcessedAt())
            .refundType(refundCalculation.getRefundType())
            .refundStatus(refundCalculation.getRefundStatus())
            .refundReason(refundCalculation.getRefundReason())
            .isRefundableByTime(refundCalculation.isRefundableByTime())
            .createdAt(refundCalculation.getCreatedAt())
            .updatedAt(refundCalculation.getUpdatedAt())
            .build();
    }
    
    /**
     * ID 조회 (하위 호환성)
     */
    public Long getId() {
        return refundCalculationId;
    }
    
    /**
     * RefundCalculation 엔티티와 RefundCalculationService를 사용하여 DTO 생성
     * 
     * @param refundCalculation 환불 계산 엔티티
     * @param calculationService 환불 계산 서비스
     * @return RefundResponseDto
     */
    public static RefundResponseDto from(RefundCalculation refundCalculation, 
                                       com.sudo.railo.payment.domain.service.RefundCalculationService calculationService) {
        return RefundResponseDto.builder()
            .refundCalculationId(refundCalculation.getId())
            .paymentId(refundCalculation.getPaymentId())
            .reservationId(refundCalculation.getReservationId())
            .memberId(refundCalculation.getMemberId())
            .originalAmount(refundCalculation.getOriginalAmount())
            .mileageUsed(refundCalculation.getMileageUsed())
            .refundFeeRate(refundCalculation.getRefundFeeRate())
            .refundFee(refundCalculation.getRefundFee())
            .refundAmount(refundCalculation.getRefundAmount())
            .mileageRefundAmount(refundCalculation.getMileageRefundAmount())
            .trainDepartureTime(refundCalculation.getTrainDepartureTime())
            .trainArrivalTime(refundCalculation.getTrainArrivalTime())
            .refundRequestTime(refundCalculation.getRefundRequestTime())
            .processedAt(refundCalculation.getProcessedAt())
            .refundType(refundCalculation.getRefundType())
            .refundStatus(refundCalculation.getRefundStatus())
            .refundReason(refundCalculation.getRefundReason())
            .isRefundableByTime(calculationService.isRefundableByTime(refundCalculation))
            .createdAt(refundCalculation.getCreatedAt())
            .updatedAt(refundCalculation.getUpdatedAt())
            .build();
    }
} 