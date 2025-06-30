package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.service.PaymentRefundService;
import com.sudo.railo.payment.exception.PaymentException;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.global.success.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 결제 환불/취소 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "결제 환불/취소", description = "결제 취소 및 환불 관련 API")
public class PaymentRefundController {
    
    private final PaymentRefundService paymentRefundService;
    
    /**
     * 결제 취소 (결제 전 취소)
     */
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "결제 취소", description = "결제 전 상태에서 결제를 취소합니다")
    public ResponseEntity<SuccessResponse<String>> cancelPayment(
            @Parameter(description = "결제 ID", required = true)
            @PathVariable @NotNull Long paymentId,
            
            @RequestBody @Valid CancelPaymentRequest request) {
        
        log.debug("결제 취소 API 호출 - paymentId: {}", paymentId);
        
        try {
            paymentRefundService.cancelPayment(paymentId, request.getReason());
            
            return ResponseEntity.ok(
                    SuccessResponse.of(null, "결제가 성공적으로 취소되었습니다")
            );
            
        } catch (PaymentValidationException e) {
            log.warn("결제 취소 검증 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 취소 처리 중 오류 발생 - paymentId: {}", paymentId, e);
            throw new PaymentException("결제 취소 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 결제 환불 (전체 환불)
     */
    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "결제 환불", description = "결제 완료 후 전체 환불을 처리합니다")
    public ResponseEntity<SuccessResponse<String>> refundPayment(
            @Parameter(description = "결제 ID", required = true)
            @PathVariable @NotNull Long paymentId,
            
            @RequestBody @Valid RefundPaymentRequest request) {
        
        log.debug("결제 환불 API 호출 - paymentId: {}", paymentId);
        
        try {
            paymentRefundService.refundPayment(paymentId, request.getReason());
            
            return ResponseEntity.ok(
                    SuccessResponse.of(null, "환불이 성공적으로 처리되었습니다")
            );
            
        } catch (PaymentValidationException e) {
            log.warn("결제 환불 검증 실패 - paymentId: {}, error: {}", paymentId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 환불 처리 중 오류 발생 - paymentId: {}", paymentId, e);
            throw new PaymentException("결제 환불 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 결제 취소 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class CancelPaymentRequest {
        
        @NotBlank(message = "취소 사유는 필수입니다")
        private String reason;
    }
    
    /**
     * 결제 환불 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class RefundPaymentRequest {
        
        @NotBlank(message = "환불 사유는 필수입니다")
        private String reason;
    }
} 