package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PG 결제 취소/환불 응답 DTO
 * 
 * Mock PG 시스템에서 반환하는 결제 취소 및 환불 결과 정보
 * 실제 PG 연동 시 각 PG사별 응답 형식에 맞게 수정 필요
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgPaymentCancelResponse {
    
    /**
     * 취소/환불 성공 여부
     */
    private boolean success;
    
    /**
     * 원본 PG 트랜잭션 ID
     */
    private String pgTransactionId;
    
    /**
     * 취소 트랜잭션 ID
     */
    private String cancelTransactionId;
    
    /**
     * 취소 승인 번호
     */
    private String cancelApprovalNo;
    
    /**
     * 가맹점 주문 ID
     */
    private String merchantOrderId;
    
    /**
     * 취소/환불 금액
     */
    private BigDecimal cancelAmount;
    
    /**
     * 잔여 금액 (전체 취소 시 0)
     */
    private BigDecimal remainingAmount;
    
    /**
     * 취소 상태 (CANCELLED, REFUNDED, FAILED)
     */
    private String cancelStatus;
    
    /**
     * 취소 타입 (FULL: 전체 취소)
     */
    private String cancelType;
    
    /**
     * 취소/환불 일시
     */
    private LocalDateTime cancelDateTime;
    
    /**
     * 환불 예정일 (계좌 환불 시)
     */
    private LocalDateTime refundScheduledDate;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 오류 코드 (실패 시)
     */
    private String errorCode;
    
    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;
    
    /**
     * 취소 사유
     */
    private String cancelReason;
    
    /**
     * 환불 수수료
     */
    private BigDecimal refundFee;
    
    /**
     * 실제 환불 금액 (환불금액 - 수수료)
     */
    private BigDecimal actualRefundAmount;
    
    /**
     * 추가 응답 정보 (각 PG사별 특수 정보)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 취소 승인 번호 (cancelApprovalNo의 alias)
     */
    private String cancelApprovalNumber;
    
    /**
     * 취소 처리 일시 (cancelDateTime의 alias)
     */
    private LocalDateTime canceledAt;
    
    /**
     * getCancelApprovalNumber 메서드 - 기존 cancelApprovalNo 반환
     */
    public String getCancelApprovalNumber() {
        return this.cancelApprovalNo != null ? this.cancelApprovalNo : this.cancelApprovalNumber;
    }
    
    /**
     * 성공 응답 생성 헬퍼 메서드 (전체 취소)
     */
    public static PgPaymentCancelResponse success(String pgTransactionId, String cancelTransactionId,
                                                String cancelApprovalNo, String merchantOrderId, 
                                                BigDecimal cancelAmount, String cancelReason) {
        return PgPaymentCancelResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .cancelTransactionId(cancelTransactionId)
                .cancelApprovalNo(cancelApprovalNo)
                .merchantOrderId(merchantOrderId)
                .cancelAmount(cancelAmount)
                .remainingAmount(BigDecimal.ZERO)
                .cancelStatus("CANCELLED")
                .cancelType("FULL")
                .cancelDateTime(LocalDateTime.now())
                .cancelReason(cancelReason)
                .refundFee(BigDecimal.ZERO)
                .actualRefundAmount(cancelAmount)
                .message("취소가 성공적으로 처리되었습니다")
                .build();
    }
    
    /**
     * 실패 응답 생성 헬퍼 메서드
     */
    public static PgPaymentCancelResponse failure(String pgTransactionId, String merchantOrderId,
                                                String errorCode, String errorMessage) {
        return PgPaymentCancelResponse.builder()
                .success(false)
                .pgTransactionId(pgTransactionId)
                .merchantOrderId(merchantOrderId)
                .cancelStatus("FAILED")
                .cancelDateTime(LocalDateTime.now())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .message("취소 처리에 실패했습니다")
                .build();
    }
} 