package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.infrastructure.external.pg.PgPaymentService;
import com.sudo.railo.payment.infrastructure.external.pg.dto.*;
import com.sudo.railo.payment.application.service.PaymentService;
import com.sudo.railo.payment.application.service.PaymentCalculationService;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentExecuteResponse;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.success.PgPaymentSuccess;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * PG 결제 연동 컨트롤러
 * 카카오페이, 네이버페이 등 외부 PG 결제 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/pg")
@RequiredArgsConstructor
public class PgPaymentController {
    
    private final PgPaymentService pgPaymentService;
    private final PaymentService paymentService;
    private final PaymentCalculationService paymentCalculationService;
    private final MemberRepository memberRepository;
    
    /**
     * PG 결제 요청 (결제창 URL 생성)
     * POST /api/v1/payments/pg/request
     */
    @PostMapping("/request")
    public ResponseEntity<SuccessResponse<PgPaymentResponse>> requestPayment(@RequestBody PgPaymentRequestDto request) {
        log.debug("PG 결제 요청: paymentMethod={}, orderId={}", request.getPaymentMethod(), request.getMerchantOrderId());
        
        // DTO를 PG 요청 객체로 변환
        PgPaymentRequest pgRequest = PgPaymentRequest.builder()
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .productName(request.getProductName())
                .buyerName(request.getBuyerName())
                .buyerEmail(request.getBuyerEmail())
                .buyerPhone(request.getBuyerPhone())
                .successUrl(request.getSuccessUrl())
                .failUrl(request.getFailUrl())
                .cancelUrl(request.getCancelUrl())
                .build();
        
        PgPaymentResponse response = pgPaymentService.requestPayment(request.getPaymentMethod(), pgRequest);
        
        return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_REQUEST_SUCCESS, response));
    }
    
    /**
     * PG 결제 승인 (결제창에서 돌아온 후 최종 승인)
     * POST /api/v1/payments/pg/approve
     */
    @PostMapping("/approve")
    public ResponseEntity<SuccessResponse<PaymentExecuteResponse>> approvePayment(@RequestBody PgPaymentApproveDto request) {
        log.debug("PG 결제 승인: paymentMethod={}, tid={}", request.getPaymentMethod(), request.getPgTransactionId());
        log.info("PG 결제 승인 요청 데이터: calculationId={}, merchantOrderId={}, memberId={}", 
                request.getCalculationId(), request.getMerchantOrderId(), request.getMemberId());
        
        // calculationId 검증 추가
        String calculationId = request.getCalculationId();
        if (calculationId == null || calculationId.trim().isEmpty()) {
            log.error("calculationId가 null이거나 비어있습니다. request.getId()={}, request.getCalculationId()={}", 
                    request.getId(), request.getCalculationId());
            throw new IllegalArgumentException("calculationId는 필수입니다.");
        }
        
        log.info("calculationId 검증 완료: calculationId={} (getId()={}, getCalculationId()={})", 
                calculationId, request.getId(), request.getCalculationId());
        
        // 비회원 정보 검증
        validateNonMemberInfo(request);
        
        // 1. PG 결제 승인
        PgPaymentResponse pgResponse = pgPaymentService.approvePayment(
                request.getPaymentMethod(),
                request.getPgTransactionId(),
                request.getMerchantOrderId()
        );
        
        log.info("PG 승인 완료: success={}, status={}", pgResponse.isSuccess(), pgResponse.getStatus());
        
        // PG 승인 완료 후 결제 데이터 저장
        if (pgResponse.isSuccess()) {
            try {
                // 1. 결제 계산 정보 조회 (마일리지 사용량 포함)
                log.info("결제 계산 정보 조회 시작 - calculationId: {} (from request.getCalculationId())", calculationId);
                PaymentCalculationResponse calculationResponse = paymentCalculationService.getCalculation(calculationId);
                log.info("결제 계산 정보 조회 완료 - calculationId: {}, 마일리지 사용: {}, 최종금액: {}", 
                        calculationResponse.getId(), 
                        calculationResponse.getMileageInfo().getUsedMileage(),
                        calculationResponse.getFinalPayableAmount());
                
                // PaymentExecuteRequest 생성 전 상세 로깅
                log.info("🔍 PaymentExecuteRequest 생성 시작 - calculationId: {}, merchantOrderId: {}, memberId: {}", 
                    calculationId, request.getMerchantOrderId(), request.getMemberId());
                log.info("📊 PaymentCalculation 정보 - reservationId: {}, externalOrderId: {}, finalAmount: {}", 
                    calculationResponse.getReservationId(), calculationResponse.getExternalOrderId(), 
                    calculationResponse.getFinalPayableAmount());
                
                // 회원 정보 처리: memberId는 실제 member.id
                Long actualMemberId = null;
                if (request.getMemberId() != null) {
                    // memberId는 이미 실제 member.id
                    actualMemberId = request.getMemberId();
                    log.info("🔍 회원 정보 확인 - memberId: {}", actualMemberId);
                    
                    // 회원 존재 여부 확인
                    final Long memberIdForLambda = actualMemberId; // lambda를 위한 final 변수
                    Member member = memberRepository.findById(actualMemberId)
                        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. ID: " + memberIdForLambda));
                    
                    log.info("✅ 회원 정보 확인 완료 - id: {}, memberNo: {}", 
                        member.getId(), 
                        member.getMemberDetail() != null ? member.getMemberDetail().getMemberNo() : "N/A");
                }
                
                PaymentExecuteRequest paymentRequest = PaymentExecuteRequest.builder()
                        .calculationId(calculationId) // request.getId() 대신 calculationId 사용
                        .idempotencyKey("pg_" + request.getPgTransactionId())
                        .paymentMethod(PaymentExecuteRequest.PaymentMethodInfo.builder()
                                .type(request.getPaymentMethod().name())
                                .pgProvider(request.getPaymentMethod().name())
                                .pgToken("mock_token_" + request.getPgTransactionId())
                                .build())
                        // 회원 정보 설정 (로그인된 경우) - 변환된 실제 ID 사용
                        .memberId(actualMemberId)
                        // 마일리지 정보 추가 (계산 결과에서 가져옴)
                        .mileageToUse(calculationResponse.getMileageInfo().getUsedMileage())
                        .availableMileage(calculationResponse.getMileageInfo().getAvailableMileage())
                        // 비회원 정보 설정
                        .nonMemberName(request.getMemberId() != null ? null : request.getNonMemberName())
                        .nonMemberPhone(request.getMemberId() != null ? null : request.getNonMemberPhone()) 
                        .nonMemberPassword(request.getMemberId() != null ? null : request.getNonMemberPassword())
                        // 현금영수증 정보 설정
                        .requestReceipt(request.getRequestReceipt() != null ? request.getRequestReceipt() : false)
                        .receiptType(request.getReceiptType())
                        .receiptPhoneNumber(request.getReceiptPhoneNumber())
                        .businessNumber(request.getBusinessNumber())
                        .build();
                
                // PaymentExecuteRequest 생성 완료 로깅
                log.info("🎯 PaymentExecuteRequest 생성 완료 - 회원타입: {}, 결제수단: {}, PG제공자: {}", 
                    request.getMemberId() != null ? "회원" : "비회원", 
                    paymentRequest.getPaymentMethod().getType(),
                    paymentRequest.getPaymentMethod().getPgProvider());
                
                // 결제 처리 및 DB 저장
                log.info("💳 PaymentService.executePayment 호출 시작");
                PaymentExecuteResponse paymentResponse = paymentService.executePayment(paymentRequest);
                
                log.info("✅ 결제 데이터 저장 완료: paymentId={}, status={}, reservationId={}", 
                        paymentResponse.getId(), paymentResponse.getPaymentStatus(), paymentResponse.getReservationId());
                
                return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_APPROVE_SUCCESS, paymentResponse));
                
            } catch (Exception e) {
                log.error("❌ 결제 데이터 저장 실패 - calculationId: {}, merchantOrderId: {}, 예외타입: {}, 오류메시지: {}", 
                    calculationId, request.getMerchantOrderId(), e.getClass().getName(), e.getMessage(), e);
                
                // 저장 실패해도 PG는 성공했으므로 응답 처리
                PaymentExecuteResponse errorResponse = PaymentExecuteResponse.builder()
                        .paymentId(null)
                        .externalOrderId(request.getMerchantOrderId())
                        .paymentStatus(com.sudo.railo.payment.domain.entity.PaymentExecutionStatus.PROCESSING)
                        .amountPaid(pgResponse.getAmount())
                        .result(PaymentExecuteResponse.PaymentResult.builder()
                                .success(true)
                                .message("PG 승인 완료 (데이터 저장 재처리 필요)")
                                .build())
                        .build();
                
                return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_APPROVE_SUCCESS, errorResponse));
            }
        } else {
            // PG 승인 실패 처리
            log.warn("PG 결제 승인 실패: code={}, message={}", pgResponse.getErrorCode(), pgResponse.getErrorMessage());
            
            PaymentExecuteResponse errorResponse = PaymentExecuteResponse.builder()
                    .paymentId(null)
                    .externalOrderId(request.getMerchantOrderId())
                    .paymentStatus(com.sudo.railo.payment.domain.entity.PaymentExecutionStatus.FAILED)
                    .amountPaid(BigDecimal.ZERO)
                    .result(PaymentExecuteResponse.PaymentResult.builder()
                            .success(false)
                            .errorCode(pgResponse.getErrorCode())
                            .message(pgResponse.getErrorMessage() != null ? pgResponse.getErrorMessage() : "결제 승인이 실패했습니다.")
                            .build())
                    .build();
            
            return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_APPROVE_SUCCESS, errorResponse));
        }
    }
    
    /**
     * PG 결제 취소/환불
     * POST /api/v1/payments/pg/cancel
     */
    @PostMapping("/cancel")
    public ResponseEntity<SuccessResponse<PgPaymentCancelResponse>> cancelPayment(@RequestBody PgPaymentCancelDto request) {
        log.debug("PG 결제 취소: paymentMethod={}, tid={}", request.getPaymentMethod(), request.getPgTransactionId());
        
        PgPaymentCancelRequest cancelRequest = PgPaymentCancelRequest.builder()
                .pgTransactionId(request.getPgTransactionId())
                .merchantOrderId(request.getMerchantOrderId())
                .cancelAmount(request.getCancelAmount())
                .cancelReason(request.getCancelReason())
                .requestedBy(request.getRequestedBy())
                .build();
        
        PgPaymentCancelResponse response = pgPaymentService.cancelPayment(request.getPaymentMethod(), cancelRequest);
        
        return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_CANCEL_SUCCESS, response));
    }
    
    /**
     * PG 결제 상태 조회
     * GET /api/v1/payments/pg/status/{paymentMethod}/{pgTransactionId}
     */
    @GetMapping("/status/{paymentMethod}/{pgTransactionId}")
    public ResponseEntity<SuccessResponse<PgPaymentResponse>> getPaymentStatus(
            @PathVariable PaymentMethod paymentMethod,
            @PathVariable String pgTransactionId) {
        log.debug("PG 결제 상태 조회: paymentMethod={}, tid={}", paymentMethod, pgTransactionId);
        
        PgPaymentResponse response = pgPaymentService.getPaymentStatus(paymentMethod, pgTransactionId);
        
        return ResponseEntity.ok(SuccessResponse.of(PgPaymentSuccess.PG_PAYMENT_STATUS_SUCCESS, response));
    }
    
    /**
     * 비회원 정보 검증
     */
    private void validateNonMemberInfo(PgPaymentApproveDto request) {
        // 비회원인 경우 비회원 정보 검증
        if (request.getMemberId() == null) {
            if (request.getNonMemberName() == null || request.getNonMemberName().trim().isEmpty()) {
                throw new IllegalArgumentException("비회원 이름은 필수입니다.");
            }
            if (request.getNonMemberPhone() == null || !request.getNonMemberPhone().matches("^[0-9]{11}$")) {
                throw new IllegalArgumentException("전화번호는 11자리 숫자여야 합니다. (예: 01012345678)");
            }
            if (request.getNonMemberPassword() == null || !request.getNonMemberPassword().matches("^[0-9]{5}$")) {
                throw new IllegalArgumentException("비밀번호는 5자리 숫자여야 합니다. (예: 12345)");
            }
            log.debug("비회원 정보 검증 완료");
        }
    }
    
    // === DTO 클래스들 ===
    
    @lombok.Data
    public static class PgPaymentRequestDto {
        private String merchantOrderId;
        private BigDecimal amount;
        private PaymentMethod paymentMethod;
        private String productName;
        private String buyerName;
        private String buyerEmail;
        private String buyerPhone;
        private String successUrl;
        private String failUrl;
        private String cancelUrl;
    }
    
    @lombok.Data
    public static class PgPaymentApproveDto {
        private PaymentMethod paymentMethod;
        private String pgTransactionId;
        private String merchantOrderId;
        private String calculationId;
        
        // 회원 정보
        private Long memberId; // 로그인된 회원 ID (비회원인 경우 null)
        
        // 비회원 정보 (비회원인 경우 필수)
        private String nonMemberName;      // 예약자 이름
        private String nonMemberPhone;     // 전화번호
        private String nonMemberPassword;  // 비밀번호
        
        // 현금영수증 정보
        private Boolean requestReceipt;
        private String receiptType; // "personal" 또는 "business"
        private String receiptPhoneNumber; // 개인 소득공제용 휴대폰 번호
        private String businessNumber; // 사업자 증빙용 사업자등록번호
        
        /**
         * ID 조회 (하위 호환성)
         */
        public String getId() {
            return calculationId;
        }
    }
    
    @lombok.Data
    public static class PgPaymentCancelDto {
        private PaymentMethod paymentMethod;
        private String pgTransactionId;
        private String merchantOrderId;
        private BigDecimal cancelAmount;
        private String cancelReason;
        private String requestedBy;
    }
} 