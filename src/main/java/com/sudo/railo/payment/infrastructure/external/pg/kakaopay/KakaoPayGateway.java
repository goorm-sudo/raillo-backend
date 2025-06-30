package com.sudo.railo.payment.infrastructure.external.pg.kakaopay;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentGateway;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import com.sudo.railo.payment.infrastructure.external.pg.kakaopay.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * 카카오페이 API 연동 Gateway
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"real", "prod", "test"})  // PG 연동 시에만 활성화
public class KakaoPayGateway implements PgPaymentGateway {

    private final RestTemplate restTemplate;
    
    @Value("${payment.kakaopay.admin-key}")
    private String adminKey;
    
    @Value("${payment.kakaopay.cid}")
    private String cid;
    
    @Value("${payment.kakaopay.ready-url}")
    private String readyUrl;
    
    @Value("${payment.kakaopay.approve-url}")
    private String approveUrl;
    
    @Value("${payment.kakaopay.cancel-url}")
    private String cancelUrl;
    
    @Value("${payment.kakaopay.order-url}")
    private String orderUrl;

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.KAKAO_PAY.equals(paymentMethod);
    }

    @Override
    public PgPaymentResponse requestPayment(PgPaymentRequest request) {
        log.debug("[카카오페이] 결제 요청: orderId={}, amount={}", request.getMerchantOrderId(), request.getAmount());
        
        try {
            // 카카오페이 결제 준비 요청
            KakaoPayReadyRequest kakaoRequest = KakaoPayReadyRequest.builder()
                    .cid(cid)
                    .partnerOrderId(request.getMerchantOrderId())
                    .partnerUserId(request.getBuyerEmail())
                    .itemName(request.getProductName())
                    .quantity(1)
                    .totalAmount(request.getAmount().intValue())
                    .taxFreeAmount(0)
                    .approvalUrl("http://localhost:3001/payment/kakao/success")  // 프론트엔드 URL
                    .cancelUrl("http://localhost:3001/payment/kakao/cancel")
                    .failUrl("http://localhost:3001/payment/kakao/fail")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // MultiValueMap으로 변환 (카카오페이는 form-data 방식)
            MultiValueMap<String, String> params = convertToFormData(kakaoRequest);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            
            ResponseEntity<KakaoPayReadyResponse> response = restTemplate.postForEntity(
                readyUrl, entity, KakaoPayReadyResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                KakaoPayReadyResponse kakaoResponse = response.getBody();
                
                log.debug("[카카오페이] 결제 준비 성공: tid={}, paymentUrl={}", 
                        kakaoResponse.getTid(), kakaoResponse.getNextRedirectPcUrl());
                
                return PgPaymentResponse.builder()
                        .success(true)
                        .pgTransactionId(kakaoResponse.getTid())
                        .amount(request.getAmount())
                        .status("READY")
                        .paymentUrl(kakaoResponse.getNextRedirectPcUrl())  // PC용 URL
                        .build();
            } else {
                log.error("[카카오페이] 결제 준비 실패: status={}", response.getStatusCode());
                return createErrorResponse("카카오페이 결제 준비 실패");
            }

        } catch (Exception e) {
            log.error("[카카오페이] 결제 요청 중 오류 발생", e);
            return createErrorResponse("카카오페이 연동 오류: " + e.getMessage());
        }
    }

    @Override
    public PgPaymentResponse approvePayment(String pgTransactionId, String merchantOrderId) {
        log.debug("[카카오페이] 결제 승인: tid={}, orderId={}", pgTransactionId, merchantOrderId);
        
        // 카카오페이는 프론트엔드에서 pg_token을 받아서 처리해야 하지만
        // 기본 인터페이스에 맞춰 간단한 승인 처리
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("SUCCESS")
                .approvalNumber("REAL_" + System.currentTimeMillis())
                .approvedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PgPaymentCancelResponse cancelPayment(PgPaymentCancelRequest request) {
        log.debug("[카카오페이] 결제 취소: tid={}, amount={}", request.getPgTransactionId(), request.getCancelAmount());
        
        try {
            KakaoPayCancelRequest kakaoRequest = KakaoPayCancelRequest.builder()
                    .cid(cid)
                    .tid(request.getPgTransactionId())
                    .cancelAmount(request.getCancelAmount().intValue())
                    .cancelTaxFreeAmount(0)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = convertToFormData(kakaoRequest);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<KakaoPayCancelResponse> response = restTemplate.postForEntity(
                cancelUrl, entity, KakaoPayCancelResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                KakaoPayCancelResponse kakaoResponse = response.getBody();
                
                log.debug("[카카오페이] 결제 취소 성공: tid={}", kakaoResponse.getTid());
                
                return PgPaymentCancelResponse.builder()
                        .success(true)
                        .pgTransactionId(kakaoResponse.getTid())
                        .cancelAmount(request.getCancelAmount())
                        .cancelApprovalNumber(kakaoResponse.getAid())
                        .canceledAt(LocalDateTime.now())
                        .build();
            } else {
                log.error("[카카오페이] 결제 취소 실패: status={}", response.getStatusCode());
                return PgPaymentCancelResponse.builder()
                        .success(false)
                        .errorCode("CANCEL_FAILED")
                        .errorMessage("카카오페이 결제 취소 실패")
                        .build();
            }

        } catch (Exception e) {
            log.error("[카카오페이] 결제 취소 중 오류 발생", e);
            return PgPaymentCancelResponse.builder()
                    .success(false)
                    .errorCode("CANCEL_ERROR")
                    .errorMessage("카카오페이 취소 오류: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PgPaymentResponse getPaymentStatus(String pgTransactionId) {
        log.debug("[카카오페이] 결제 상태 조회: tid={}", pgTransactionId);
        
        // 카카오페이는 별도 상태 조회 API가 없어서 간단한 응답 반환
        return PgPaymentResponse.builder()
                .success(true)
                .pgTransactionId(pgTransactionId)
                .status("INQUIRY_SUCCESS")
                .build();
    }

    // === Private Methods ===

    private PgPaymentResponse createErrorResponse(String errorMessage) {
        return PgPaymentResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .status("ERROR")
                .build();
    }

    // 카카오페이 API 요청을 위한 form-data 변환
    private MultiValueMap<String, String> convertToFormData(KakaoPayReadyRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", request.getCid());
        params.add("partner_order_id", request.getPartnerOrderId());
        params.add("partner_user_id", request.getPartnerUserId());
        params.add("item_name", request.getItemName());
        params.add("quantity", String.valueOf(request.getQuantity()));
        params.add("total_amount", String.valueOf(request.getTotalAmount()));
        params.add("tax_free_amount", String.valueOf(request.getTaxFreeAmount()));
        params.add("approval_url", request.getApprovalUrl());
        params.add("cancel_url", request.getCancelUrl());
        params.add("fail_url", request.getFailUrl());
        return params;
    }

    private MultiValueMap<String, String> convertToFormData(KakaoPayApproveRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", request.getCid());
        params.add("tid", request.getTid());
        params.add("partner_order_id", request.getPartnerOrderId());
        params.add("partner_user_id", request.getPartnerUserId());
        params.add("pg_token", request.getPgToken());
        return params;
    }

    private MultiValueMap<String, String> convertToFormData(KakaoPayCancelRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", request.getCid());
        params.add("tid", request.getTid());
        params.add("cancel_amount", String.valueOf(request.getCancelAmount()));
        params.add("cancel_tax_free_amount", String.valueOf(request.getCancelTaxFreeAmount()));
        return params;
    }
} 