package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KakaoPayOrderRequest {
    private String cid;
    private String tid;
} 