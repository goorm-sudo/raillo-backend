package com.sudo.railo.payment.infrastructure.external.pg.naverpay;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * 네이버페이 API 연동 Gateway
 * prod, dev 프로파일에서만 활성화
 */
@Slf4j
@Component
@Profile({"prod", "dev", "real"}) // Mock과 구분하기 위한 프로파일
@RequiredArgsConstructor
public class NaverPayGateway implements PgPaymentGateway {
    
    private final RestTemplate restTemplate;
    
    @Value("${payment.naverpay.client-id}")
    private String clientId;
    
    @Value("${payment.naverpay.client-secret}")
    private String clientSecret;
    
    private static final String NAVER_PAY_BASE_URL = "https://dev.apis.naver.com/naverpay-partner/naverpay/payments/v1";
    
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.NAVER_PAY == paymentMethod;
    }
    
    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.info("[네이버페이] 결제 요청: orderId={}, amount={}", 
                request.getMerchantOrderId(), request.getAmount());
        
        try {
            // 네이버페이 결제 요청 로직
            // API 호출 구현
            
            return PgPaymentResponse.builder()
                    .success(true)
                    .pgTransactionId("NAVER_" + System.currentTimeMillis())
                    .merchantOrderId(request.getMerchantOrderId())
                    .amount(request.getAmount())
                    .status("READY")
                    .paymentUrl("https://pay.naver.com/redirect?paymentId=NAVER_" + System.currentTimeMillis())
                    .rawResponse("Naver Pay Ready Success")
                    .build();
                    
        } catch (Exception e) {
            log.error("[네이버페이] 결제 요청 실패", e);
            return PgPaymentResponse.builder()
                    .success(false)
                    .errorCode("NAVER_PAY_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.info("[네이버페이] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        try {
            // 네이버페이 승인 로직 구현
            
            return PgPaymentResponse.builder()
                    .success(true)
                    .pgTransactionId(pgTransactionId)
                    .status("SUCCESS")
                    .approvalNumber("NAVER_REAL_" + System.currentTimeMillis())
                    .approvedAt(LocalDateTime.now())
                    .rawResponse("Naver Pay Approve Success")
                    .build();
                    
        } catch (Exception e) {
            log.error("[네이버페이] 결제 승인 실패", e);
            return PgPaymentResponse.builder()
                    .success(false)
                    .errorCode("NAVER_PAY_APPROVE_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.info("[네이버페이] 결제 취소: tid={}", request.getPgTransactionId());
        
        try {
            // 네이버페이 취소 로직 구현
            
            return PgPaymentCancelResponse.builder()
                    .success(true)
                    .pgTransactionId(request.getPgTransactionId())
                    .cancelAmount(request.getCancelAmount())
                    .cancelApprovalNumber("NAVER_CANCEL_" + System.currentTimeMillis())
                    .canceledAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("[네이버페이] 결제 취소 실패", e);
            return PgPaymentCancelResponse.builder()
                    .success(false)
                    .errorCode("NAVER_PAY_CANCEL_ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.info("[네이버페이] 결제 상태 조회: tid={}", pgTransactionId);
        
        // 네이버페이 상태 조회 로직 구현
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .rawResponse("Naver Pay Status Success")
                .build();
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        return headers;
    }
} 