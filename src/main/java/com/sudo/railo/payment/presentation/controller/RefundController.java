package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.RefundRequestDto;
import com.sudo.railo.payment.application.dto.response.RefundResponseDto;
import com.sudo.railo.payment.application.service.RefundService;
import com.sudo.railo.payment.domain.entity.RefundCalculation;
import com.sudo.railo.payment.domain.service.RefundCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 환불 관련 REST API Controller
 */
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Slf4j
public class RefundController {
    
    private final RefundService refundService;
    private final RefundCalculationService refundCalculationService;
    
    /**
     * 환불 계산 및 요청
     */
    @PostMapping("/calculate")
    public ResponseEntity<RefundResponseDto> calculateRefund(@Valid @RequestBody RefundRequestDto request) {
        log.info("환불 계산 요청 - paymentId: {}, refundType: {}", 
                request.getId(), request.getRefundType());
        
        RefundCalculation refundCalculation = refundService.calculateRefund(
            request.getId(),
            request.getRefundType(),
            request.getTrainDepartureTime(),
            request.getTrainArrivalTime(),
            request.getRefundReason(),
            request.getIdempotencyKey()
        );
        
        RefundResponseDto response = RefundResponseDto.from(refundCalculation, refundCalculationService);
        
        log.info("환불 계산 완료 - refundCalculationId: {}, refundAmount: {}", 
                response.getId(), response.getRefundAmount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 환불 처리 실행
     */
    @PostMapping("/{refundCalculationId}/process")
    public ResponseEntity<Void> processRefund(@PathVariable Long refundCalculationId) {
        log.info("환불 처리 요청 - refundCalculationId: {}", refundCalculationId);
        
        refundService.processRefund(refundCalculationId);
        
        log.info("환불 처리 완료 - refundCalculationId: {}", refundCalculationId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 환불 계산 조회
     */
    @GetMapping("/{refundCalculationId}")
    public ResponseEntity<RefundResponseDto> getRefundCalculation(@PathVariable Long refundCalculationId) {
        log.info("환불 계산 조회 - refundCalculationId: {}", refundCalculationId);
        
        return refundService.getRefundCalculation(refundCalculationId)
            .map(calculation -> RefundResponseDto.from(calculation, refundCalculationService))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 결제별 환불 계산 조회
     */
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<RefundResponseDto> getRefundCalculationByPaymentId(@PathVariable Long paymentId) {
        log.info("결제별 환불 계산 조회 - paymentId: {}", paymentId);
        
        return refundService.getRefundCalculationByPaymentId(paymentId)
            .map(calculation -> RefundResponseDto.from(calculation, refundCalculationService))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 회원별 환불 내역 조회
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<RefundResponseDto>> getRefundHistoryByMember(@PathVariable Long memberId) {
        log.info("회원별 환불 내역 조회 - memberId: {}", memberId);
        
        List<RefundCalculation> refundHistory = refundService.getRefundHistoryByMember(memberId);
        List<RefundResponseDto> response = refundHistory.stream()
            .map(calculation -> RefundResponseDto.from(calculation, refundCalculationService))
            .collect(Collectors.toList());
        
        log.info("회원별 환불 내역 조회 완료 - memberId: {}, count: {}", memberId, response.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 처리 대기 중인 환불 목록 조회 (관리자용)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<RefundResponseDto>> getPendingRefunds() {
        log.info("처리 대기 중인 환불 목록 조회");
        
        List<RefundCalculation> pendingRefunds = refundService.getPendingRefunds();
        List<RefundResponseDto> response = pendingRefunds.stream()
            .map(calculation -> RefundResponseDto.from(calculation, refundCalculationService))
            .collect(Collectors.toList());
        
        log.info("처리 대기 중인 환불 목록 조회 완료 - count: {}", response.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 환불 취소
     */
    @PostMapping("/{refundCalculationId}/cancel")
    public ResponseEntity<Void> cancelRefund(
            @PathVariable Long refundCalculationId,
            @RequestParam String reason) {
        log.info("환불 취소 요청 - refundCalculationId: {}, reason: {}", refundCalculationId, reason);
        
        refundService.cancelRefund(refundCalculationId, reason);
        
        log.info("환불 취소 완료 - refundCalculationId: {}", refundCalculationId);
        
        return ResponseEntity.ok().build();
    }
} 