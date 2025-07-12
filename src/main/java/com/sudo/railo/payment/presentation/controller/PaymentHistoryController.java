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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

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
    
    /**
     * 회원 결제 내역 조회 (페이징)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/member")
    @Operation(summary = "회원 결제 내역 조회", description = "현재 로그인한 회원의 결제 내역을 페이징으로 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getMemberPaymentHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("회원 결제 내역 조회 요청 - 회원번호: {}, 페이지: {}", userDetails.getUsername(), pageable);
        
        // 전체 기간 조회 (startDate, endDate, paymentMethod는 null)
        PaymentHistoryResponse response = paymentHistoryService.getPaymentHistory(
                userDetails, null, null, null, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회원 결제 내역 기간별 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/member/date-range")
    @Operation(summary = "회원 결제 내역 기간별 조회", description = "현재 로그인한 회원의 특정 기간 결제 내역을 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getMemberPaymentHistoryByDateRange(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회 시작일 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "결제 방법 (선택사항)", example = "KAKAO_PAY")
            @RequestParam(required = false) String paymentMethod,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("회원 기간별 결제 내역 조회 요청 - 회원번호: {}, 기간: {} ~ {}", userDetails.getUsername(), startDate, endDate);
        
        PaymentHistoryResponse response = paymentHistoryService.getPaymentHistory(
                userDetails, startDate, endDate, paymentMethod, pageable);
        
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
     * 비회원 전체 결제 내역 조회
     * 이름, 전화번호, 비밀번호로 모든 예약 조회 (JWT 토큰 불필요)
     */
    @GetMapping("/guest/all")
    @Operation(summary = "비회원 전체 결제 내역 조회", description = "비회원의 정보로 모든 결제 내역을 조회합니다")
    public ResponseEntity<PaymentHistoryResponse> getAllGuestPaymentHistory(
            @Parameter(description = "비회원 이름", required = true)
            @RequestParam @NotBlank String name,
            
            @Parameter(description = "비회원 전화번호", required = true)
            @RequestParam @NotBlank String phoneNumber,
            
            @Parameter(description = "비밀번호", required = true)
            @RequestParam @NotBlank String password,
            
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("비회원 전체 결제 내역 조회 요청 - 이름: {}, 전화번호: {}", name, phoneNumber);
        
        PaymentHistoryResponse response = paymentHistoryService.getAllNonMemberPayments(
                name, phoneNumber, password, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 결제 상세 정보 조회
     * JWT 토큰에서 memberId를 자동으로 추출하여 소유권 검증
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 상세 정보 조회", description = "특정 결제의 상세 정보를 조회합니다")
    public ResponseEntity<PaymentInfoResponse> getPaymentDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "결제 ID", required = true)
            @PathVariable @NotNull Long paymentId) {
        
        log.debug("결제 상세 정보 조회 요청 - 결제ID: {}, 회원번호: {}", paymentId, userDetails.getUsername());
        
        PaymentInfoResponse response = paymentHistoryService.getPaymentDetail(paymentId, userDetails);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 예약번호로 결제 정보 조회 (회원용)
     * JWT 토큰에서 memberId를 자동으로 추출하여 소유권 검증
     */
    @GetMapping("/member/reservation/{reservationId}")
    @Operation(summary = "예약번호로 결제 정보 조회", description = "특정 예약번호의 결제 정보를 조회합니다")
    public ResponseEntity<PaymentInfoResponse> getPaymentByReservationId(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "예약번호", required = true)
            @PathVariable @NotNull Long reservationId) {
        
        log.debug("예약번호로 결제 정보 조회 요청 - 예약번호: {}, 회원번호: {}", reservationId, userDetails.getUsername());
        
        PaymentInfoResponse response = paymentHistoryService.getPaymentByReservationId(reservationId, userDetails);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 예약번호로 결제 정보 조회 (비회원/회원 공용)
     * 회원인 경우 토큰으로 검증, 비회원인 경우 검증 없이 조회
     */
    @GetMapping("/reservation/{reservationId}")
    @Operation(summary = "예약번호로 결제 정보 조회 (공용)", description = "예약번호로 결제 정보를 조회합니다. 비회원도 사용 가능합니다.")
    public ResponseEntity<PaymentInfoResponse> getPaymentByReservationIdPublic(
            @Parameter(description = "예약번호", required = true)
            @PathVariable @NotNull Long reservationId) {
        
        log.debug("예약번호로 결제 정보 조회 요청 (공용) - 예약번호: {}", reservationId);
        
        // 비회원도 조회 가능하도록 memberId 없이 호출
        PaymentInfoResponse response = paymentHistoryService.getPaymentByReservationIdPublic(reservationId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 회원 마일리지 잔액 조회 (추가 기능)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/mileage/balance")
    @Operation(summary = "마일리지 잔액 조회", description = "현재 로그인한 회원의 마일리지 잔액을 조회합니다")
    public ResponseEntity<MileageBalanceResponse> getMileageBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("마일리지 잔액 조회 요청 - 회원번호: {}", userDetails.getUsername());
        
        // MileageBalanceService를 통한 실제 잔액 조회
        MileageBalanceInfo balanceInfo = 
                mileageBalanceService.getMileageBalance(userDetails);
        
        MileageBalanceResponse response = MileageBalanceResponse.builder()
                .memberId(balanceInfo.getMemberId())
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