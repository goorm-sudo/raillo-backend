package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Data;

@Data
public class KakaoPayOrderResponse {
    private String tid;
    private String cid;
    private String status;
    private String partnerOrderId;
    private String partnerUserId;
    private String paymentMethodType;
    private KakaoPayApproveResponse.AmountInfo amount;
    private KakaoPayApproveResponse.AmountInfo canceledAmount;
    private KakaoPayApproveResponse.AmountInfo cancelAvailableAmount;
    private String itemName;
    private String itemCode;
    private Integer quantity;
    private String createdAt;
    private String approvedAt;
    private String canceledAt;
} 