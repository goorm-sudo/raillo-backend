package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KakaoPayApproveResponse {
    private String aid;
    private String tid;
    private String cid;
    private String partnerOrderId;
    private String partnerUserId;
    private String paymentMethodType;
    private AmountInfo amount;
    private LocalDateTime approvedAt;
    
    @Data
    public static class AmountInfo {
        private BigDecimal total;
        private BigDecimal taxFree;
        private BigDecimal vat;
        private BigDecimal point;
        private BigDecimal discount;
    }
} 