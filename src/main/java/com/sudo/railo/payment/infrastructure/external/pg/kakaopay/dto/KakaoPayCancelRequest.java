package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 카카오페이 Cancel 요청 DTO
 */
@Data
@Builder
public class KakaoPayCancelRequest {
    
    private String cid;                 // 가맹점 코드
    private String tid;                 // 결제 고유번호
    private Integer cancelAmount;       // 취소 금액
    private Integer cancelTaxFreeAmount; // 취소 비과세 금액
} 