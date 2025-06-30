package com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto;

import lombok.Data;

/**
 * 카카오페이 Ready 응답 DTO
 */
@Data
public class KakaoPayReadyResponse {
    private String tid;                    // 결제 고유번호
    private String nextRedirectAppUrl;     // 요청한 클라이언트가 모바일 앱일 경우 
    private String nextRedirectMobileUrl;  // 요청한 클라이언트가 모바일 웹일 경우
    private String nextRedirectPcUrl;      // 요청한 클라이언트가 PC 웹일 경우
    private String androidAppScheme;       // 카카오페이 결제화면으로 이동하는 Android 앱 스킴
    private String iosAppScheme;           // 카카오페이 결제화면으로 이동하는 iOS 앱 스킴
    private String createdAt;             // 결제 준비 요청 시각
} 