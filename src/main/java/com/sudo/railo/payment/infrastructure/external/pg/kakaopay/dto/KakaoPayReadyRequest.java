package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 카카오페이 Ready 요청 DTO
 */
@Data
@Builder
public class KakaoPayReadyRequest {
    private String cid;                // 가맹점 코드
    private String partnerOrderId;     // 가맹점 주문번호
    private String partnerUserId;      // 가맹점 회원 id
    private String itemName;           // 상품명
    private Integer quantity;          // 상품 수량
    private Integer totalAmount;       // 상품 총액
    private Integer vatAmount;         // 상품 부가세금액
    private Integer taxFreeAmount;     // 상품 비과세금액
    private String approvalUrl;        // 결제성공시 redirect url
    private String cancelUrl;          // 결제취소시 redirect url
    private String failUrl;            // 결제실패시 redirect url
} 