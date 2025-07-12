package com.sudo.railo.payment.infrastructure.external.pg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * PG 결제 응답 DTO
 * 
 * Mock PG 시스템에서 반환하는 결제 결과 정보
 * 실제 PG 연동 시 각 PG사별 응답 형식에 맞게 수정 필요
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgPaymentResponse {
    
    /**
     * 결제 성공 여부
     */
    private boolean success;
    
    /**
     * PG 트랜잭션 ID
     */
    private String pgTransactionId;
    
    /**
     * PG 승인 번호
     */
    private String pgApprovalNo;
    
    /**
     * 가맹점 주문 ID
     */
    private String merchantOrderId;
    
    /**
     * 결제 금액
     */
    private BigDecimal amount;
    
    /**
     * 결제 상태 (SUCCESS, FAILED, PENDING, CANCELLED)
     */
    private String paymentStatus;
    
    /**
     * 결제 수단 정보
     */
    private String paymentMethodType;
    private String cardCompany;
    private String cardNumber; // 마스킹된 카드번호
    private String installmentPlan;
    
    /**
     * 결제 일시
     */
    private LocalDateTime paymentDateTime;
    
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
     * 현금영수증 정보
     */
    private String cashReceiptUrl;
    private String cashReceiptApprovalNo;
    
    /**
     * 추가 응답 정보 (각 PG사별 특수 정보)
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 결제 상태 코드 (READY, SUCCESS, FAILED 등)
     */
    private String status;
    
    /**
     * 결제 진행 URL (결제창 리다이렉트 URL)
     */
    private String paymentUrl;
    
    /**
     * 승인 번호 (pgApprovalNo의 alias)
     */
    private String approvalNumber;
    
    /**
     * 승인 일시
     */
    private LocalDateTime approvedAt;
    
    /**
     * PG사 원본 응답 데이터
     */
    private String rawResponse;
    
    /**
     * 성공 응답 생성 헬퍼 메서드
     */
    public static PgPaymentResponse success(String pgTransactionId, String pgApprovalNo, 
                                          String merchantOrderId, BigDecimal amount) {
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .pgApprovalNo(pgApprovalNo)
                .merchantOrderId(merchantOrderId)
                .amount(amount)
                .paymentStatus("SUCCESS")
                .paymentDateTime(LocalDateTime.now())
                .message("결제가 성공적으로 처리되었습니다")
                .build();
    }
    
    /**
     * 실패 응답 생성 헬퍼 메서드
     */
    public static PgPaymentResponse failure(String merchantOrderId, String errorCode, String errorMessage) {
        return PgPaymentResponse.builder()
                .success(false)
                .merchantOrderId(merchantOrderId)
                .paymentStatus("FAILED")
                .paymentDateTime(LocalDateTime.now())
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .message("결제 처리에 실패했습니다")
                .build();
    }
} 