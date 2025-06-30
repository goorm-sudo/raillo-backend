package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.response.MileageBalanceInfo;
import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.application.service.PaymentHistoryService;
import com.sudo.railo.payment.application.service.MileageBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 결제 내역 조회 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "결제 내역 조회", description = "결제 내역 조회 관련 API")
public class PaymentHistoryController {
    
    private final PaymentHistoryService paymentHistoryService;
    private final MileageBalanceService mileageBalanceService;
    
    /**
     * 회원 결제 내역 조회
     */
    @GetMapping("/history")
    @Operation(summary = "회원 결제 내역 조회", description = "회원의 결제 내역을 기간별로 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getPaymentHistory(
            @Parameter(description = "회원 ID", required = true)
            @RequestParam @NotNull Long memberId,
            
            @Parameter(description = "조회 시작일 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "결제 방법 (선택사항)", example = "KAKAO_PAY")
            @RequestParam(required = false) String paymentMethod,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("회원 결제 내역 조회 요청 - 회원ID: {}, 기간: {} ~ {}, 결제방법: {}", memberId, startDate, endDate, paymentMethod);
        
        PaymentHistoryResponse response = paymentHistoryService.getPaymentHistory(
                memberId, startDate, endDate, paymentMethod, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 비회원 결제 내역 조회
     */
    @GetMapping("/non-member")
    @Operation(summary = "비회원 결제 내역 조회", description = "비회원의 결제 내역을 조회합니다")
    public ResponseEntity<PaymentInfoResponse> getNonMemberPayment(
            @Parameter(description = "예약 번호", required = true)
            @RequestParam @NotBlank String reservationId,
            
            @Parameter(description = "예약자 이름", required = true)
            @RequestParam @NotBlank String name,
            
            @Parameter(description = "전화번호", required = true)
            @RequestParam @NotBlank String phoneNumber,
            
            @Parameter(description = "비밀번호", required = true)
            @RequestParam @NotBlank String password) {
        
        log.debug("비회원 결제 내역 조회 요청 - 예약번호: {}, 이름: {}", reservationId, name);
        
        PaymentInfoResponse response = paymentHistoryService.getNonMemberPayment(
                Long.valueOf(reservationId), name, phoneNumber, password);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 결제 상세 정보 조회 (회원용)
     */
    @GetMapping("/{paymentId}/detail")
    @Operation(summary = "결제 상세 정보 조회", description = "특정 결제의 상세 정보를 조회합니다 (마일리지 거래 내역 포함)")
    public ResponseEntity<PaymentInfoResponse> getPaymentDetail(
            @Parameter(description = "결제 ID", required = true)
            @PathVariable @NotNull Long paymentId,
            
            @Parameter(description = "회원 ID", required = true)
            @RequestParam @NotNull Long memberId) {
        
        log.debug("결제 상세 정보 조회 요청 - 결제ID: {}, 회원ID: {}", paymentId, memberId);
        
        PaymentInfoResponse response = paymentHistoryService.getPaymentDetail(paymentId, memberId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회원 마일리지 잔액 조회 (추가 기능)
     */
    @GetMapping("/mileage/balance")
    @Operation(summary = "마일리지 잔액 조회", description = "회원의 현재 마일리지 잔액을 조회합니다")
    public ResponseEntity<MileageBalanceResponse> getMileageBalance(
            @Parameter(description = "회원 ID", required = true)
            @RequestParam @NotNull Long memberId) {
        
        log.debug("마일리지 잔액 조회 요청 - 회원ID: {}", memberId);
        
        // MileageBalanceService를 통한 실제 잔액 조회
        MileageBalanceInfo balanceInfo = 
                mileageBalanceService.getMileageBalance(memberId);
        
        MileageBalanceResponse response = MileageBalanceResponse.builder()
                .memberId(memberId)
                .currentBalance(balanceInfo.getCurrentBalance())
                .activeBalance(balanceInfo.getActiveBalance())
                .lastUpdatedAt(balanceInfo.getLastTransactionAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 마일리지 잔액 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class MileageBalanceResponse {
        private Long memberId;
        private java.math.BigDecimal currentBalance;    // 현재 총 잔액
        private java.math.BigDecimal activeBalance;     // 활성 잔액 (만료되지 않은 것만)
        private java.time.LocalDateTime lastUpdatedAt; // 마지막 업데이트 시간
    }
} 