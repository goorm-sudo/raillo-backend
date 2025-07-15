package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KakaoPayApproveRequest {
    private String cid;
    private String tid;
    private String partnerOrderId;
    private String partnerUserId;
    private String pgToken;
} 