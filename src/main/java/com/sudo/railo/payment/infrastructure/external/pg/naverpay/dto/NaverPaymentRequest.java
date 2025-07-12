package com.sudo.railo.payment.infrastructure.external.pg.naverpay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NaverPaymentRequest {
    private String merchantPayKey;
    private String productName;
    private Long totalPayAmount;
    private String returnUrl;
    private ProductItem[] productItems;
    
    @Data
    @Builder
    public static class ProductItem {
        private String categoryType;
        private String categoryId;
        private String uid;
        private String name;
        private Integer count;
        private String payReferrer;
    }
} 