package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Data;

/**
 * 카카오페이 Cancel 응답 DTO
 */
@Data
public class KakaoPayCancelResponse {
    
    private String aid;                 // 요청 고유 번호
    private String tid;                 // 결제 고유 번호
    private String cid;                 // 가맹점 코드
    private String status;              // 결제 상태
    private String partnerOrderId;      // 가맹점 주문번호
    private String partnerUserId;       // 가맹점 회원 id
    private String paymentMethodType;   // 결제 수단
    private Amount amount;              // 결제 금액 정보
    private String createdAt;           // 결제 준비 요청 시각
    private String approvedAt;          // 결제 승인 시각
    private String canceledAt;          // 결제 취소 시각
    
    @Data
    public static class Amount {
        private Integer total;          // 전체 결제 금액
        private Integer taxFree;        // 비과세 금액
        private Integer vat;            // 부가세 금액
        private Integer point;          // 사용한 포인트
        private Integer discount;       // 할인금액
    }
} 