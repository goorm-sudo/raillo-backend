package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.response.MileageBalanceInfo;
import com.sudo.railo.payment.application.dto.response.PaymentHistoryResponse;
import com.sudo.railo.payment.application.dto.response.PaymentInfoResponse;
import com.sudo.railo.payment.application.service.PaymentHistoryService;
import com.sudo.railo.payment.application.service.MileageBalanceService;
import com.sudo.railo.member.application.util.MemberUtil;
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
 * JWT 토큰에서 회원 정보를 자동으로 추출하여 사용
 */
@RestController
@RequestMapping("/api/v1/payment-history")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "결제 내역 관리", description = "결제 내역 조회, 마일리지 잔액 조회 등 관련 API")
public class PaymentHistoryController {
    
    private final PaymentHistoryService paymentHistoryService;
    private final MileageBalanceService mileageBalanceService;
    private final MemberUtil memberUtil;
    
    /**
     * 회원 결제 내역 조회 (페이징)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/member")
    @Operation(summary = "회원 결제 내역 조회", description = "현재 로그인한 회원의 결제 내역을 페이징으로 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getMemberPaymentHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("회원 결제 내역 조회 요청 - 회원ID: {}, 페이지: {}", memberId, pageable);
        
        // 전체 기간 조회 (startDate, endDate, paymentMethod는 null)
        PaymentHistoryResponse response = paymentHistoryService.getPaymentHistory(
                memberId, null, null, null, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회원 결제 내역 기간별 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/member/date-range")
    @Operation(summary = "회원 결제 내역 기간별 조회", description = "현재 로그인한 회원의 특정 기간 결제 내역을 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getMemberPaymentHistoryByDateRange(
            @Parameter(description = "조회 시작일 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "결제 방법 (선택사항)", example = "KAKAO_PAY")
            @RequestParam(required = false) String paymentMethod,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("회원 기간별 결제 내역 조회 요청 - 회원ID: {}, 기간: {} ~ {}", memberId, startDate, endDate);
        
        PaymentHistoryResponse response = paymentHistoryService.getPaymentHistory(
                memberId, startDate, endDate, paymentMethod, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 비회원 결제 내역 조회
     * 예약번호, 이름, 전화번호, 비밀번호로 조회 (JWT 토큰 불필요)
     */
    @GetMapping("/guest")
    @Operation(summary = "비회원 결제 내역 조회", description = "비회원의 예약정보로 결제 내역을 조회합니다")
    public ResponseEntity<PaymentInfoResponse> getGuestPaymentHistory(
            @Parameter(description = "예약번호", required = true)
            @RequestParam @NotNull Long reservationId,
            
            @Parameter(description = "비회원 이름", required = true)
            @RequestParam @NotBlank String name,
            
            @Parameter(description = "비회원 전화번호", required = true)
            @RequestParam @NotBlank String phoneNumber,
            
            @Parameter(description = "비밀번호", required = true)
            @RequestParam @NotBlank String password) {
        
        log.debug("비회원 결제 내역 조회 요청 - 예약번호: {}, 이름: {}", reservationId, name);
        
        PaymentInfoResponse response = paymentHistoryService.getNonMemberPayment(
                reservationId, name, phoneNumber, password);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 결제 상세 정보 조회
     * JWT 토큰에서 memberId를 자동으로 추출하여 소유권 검증
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 상세 정보 조회", description = "특정 결제의 상세 정보를 조회합니다")
    public ResponseEntity<PaymentInfoResponse> getPaymentDetail(
            @Parameter(description = "결제 ID", required = true)
            @PathVariable @NotNull Long paymentId) {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
        log.debug("결제 상세 정보 조회 요청 - 결제ID: {}, 회원ID: {}", paymentId, memberId);
        
        PaymentInfoResponse response = paymentHistoryService.getPaymentDetail(paymentId, memberId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회원 마일리지 잔액 조회 (추가 기능)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/mileage/balance")
    @Operation(summary = "마일리지 잔액 조회", description = "현재 로그인한 회원의 마일리지 잔액을 조회합니다")
    public ResponseEntity<MileageBalanceResponse> getMileageBalance() {
        
        // JWT 토큰에서 현재 로그인한 사용자의 memberId 추출
        Long memberId = memberUtil.getCurrentMemberId();
        
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