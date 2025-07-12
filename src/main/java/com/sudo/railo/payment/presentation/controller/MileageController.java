package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.payment.application.dto.response.MileageBalanceInfo;
import com.sudo.railo.payment.application.dto.response.MileageStatisticsResponse;
import com.sudo.railo.payment.application.dto.response.MileageTransactionResponse;
import com.sudo.railo.payment.application.service.MileageBalanceService;
import com.sudo.railo.payment.application.port.in.QueryMileageEarningUseCase;
import com.sudo.railo.payment.application.service.MileageTransactionService;
import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.success.PaymentSuccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 마일리지 조회 및 관리 API 컨트롤러
 * JWT 토큰에서 회원 정보를 자동으로 추출하여 사용
 */
@RestController
@RequestMapping("/api/v1/mileage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "마일리지 관리", description = "마일리지 조회, 거래내역, 통계 등 관련 API")
public class MileageController {
    
    private final MileageBalanceService mileageBalanceService;
    private final MileageTransactionService mileageTransactionService;
    private final QueryMileageEarningUseCase queryMileageEarningUseCase;
    private final MemberRepository memberRepository;
    
    /**
     * 마일리지 잔액 조회 (상세)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/balance")
    @Operation(summary = "마일리지 잔액 조회", description = "현재 로그인한 회원의 마일리지 잔액을 조회합니다")
    public ResponseEntity<SuccessResponse<MileageBalanceResponse>> getMileageBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("마일리지 잔액 조회 - 회원번호: {}", userDetails.getUsername());
        
        MileageBalanceInfo balanceInfo = mileageBalanceService.getMileageBalance(userDetails);
        
        MileageBalanceResponse response = MileageBalanceResponse.builder()
                .memberId(balanceInfo.getMemberId())
                .currentBalance(balanceInfo.getCurrentBalance())
                .activeBalance(balanceInfo.getActiveBalance())
                .pendingEarning(balanceInfo.getPendingEarning())
                .expiringInMonth(balanceInfo.getExpiringInMonth())
                .lastUpdatedAt(balanceInfo.getLastTransactionAt())
                .build();
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_INQUIRY_SUCCESS, response));
    }
    
    /**
     * 마일리지 잔액 조회 (간단)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/balance/simple")
    @Operation(summary = "마일리지 잔액 간단 조회", description = "현재 로그인한 회원의 마일리지 잔액만 간단히 조회합니다")
    public ResponseEntity<SuccessResponse<SimpleMileageBalanceResponse>> getSimpleMileageBalance(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("마일리지 간단 잔액 조회 - 회원번호: {}", userDetails.getUsername());
        
        MileageBalanceInfo balanceInfo = mileageBalanceService.getMileageBalance(userDetails);
        
        SimpleMileageBalanceResponse response = SimpleMileageBalanceResponse.builder()
                .balance(balanceInfo.getCurrentBalance())
                .build();
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_INQUIRY_SUCCESS, response));
    }
    
    /**
     * 사용 가능한 마일리지 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/available")
    @Operation(summary = "사용 가능한 마일리지 조회", description = "현재 사용 가능한 마일리지를 조회합니다")
    public ResponseEntity<SuccessResponse<AvailableMileageResponse>> getAvailableMileage(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("사용 가능한 마일리지 조회 - 회원번호: {}", userDetails.getUsername());
        
        MileageBalanceInfo balanceInfo = mileageBalanceService.getMileageBalance(userDetails);
        
        AvailableMileageResponse response = AvailableMileageResponse.builder()
                .availableBalance(balanceInfo.getActiveBalance())
                .minimumUsableAmount(BigDecimal.valueOf(1000)) // 최소 사용 금액
                .maximumUsablePercentage(50) // 최대 사용 비율 50%
                .build();
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_INQUIRY_SUCCESS, response));
    }
    
    /**
     * 마일리지 통계 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/statistics")
    @Operation(summary = "마일리지 통계 조회", description = "지정 기간의 마일리지 통계를 조회합니다")
    public ResponseEntity<SuccessResponse<MileageStatisticsResponse>> getMileageStatistics(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회 시작일", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.debug("마일리지 통계 조회 - 회원번호: {}, 기간: {} ~ {}", userDetails.getUsername(), startDate, endDate);
        
        MileageStatisticsResponse statistics = 
                mileageTransactionService.getMileageStatistics(userDetails, startDate, endDate);
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.STATISTICS_INQUIRY_SUCCESS, statistics));
    }
    
    /**
     * 마일리지 거래 내역 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/transactions")
    @Operation(summary = "마일리지 거래 내역 조회", description = "마일리지 적립/사용 내역을 페이징으로 조회합니다")
    public ResponseEntity<SuccessResponse<MileageTransactionListResponse>> getMileageTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("마일리지 거래 내역 조회 - 회원번호: {}, 페이지: {}, 크기: {}", userDetails.getUsername(), page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<MileageTransaction> transactionPage = 
                mileageTransactionService.getMileageTransactions(userDetails, pageable);
        
        MileageTransactionListResponse response = MileageTransactionListResponse.builder()
                .transactions(transactionPage.getContent())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .hasNext(transactionPage.hasNext())
                .hasPrevious(transactionPage.hasPrevious())
                .build();
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_TRANSACTION_INQUIRY_SUCCESS, response));
    }
    
    /**
     * 적립 예정 마일리지 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/earning-schedules")
    @Operation(summary = "적립 예정 마일리지 조회", description = "아직 적립되지 않은 예정 마일리지를 조회합니다")
    public ResponseEntity<SuccessResponse<List<MileageEarningSchedule>>> getEarningSchedules(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("적립 예정 마일리지 조회 - 회원번호: {}", userDetails.getUsername());
        
        // UserDetails에서 회원 정보 추출
        Member member = memberRepository.findByMemberNo(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
        Long memberId = member.getId();
        
        List<MileageEarningSchedule> schedules = 
                queryMileageEarningUseCase.getEarningSchedulesByMemberId(
                        memberId, MileageEarningSchedule.EarningStatus.SCHEDULED);
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_SCHEDULE_INQUIRY_SUCCESS, schedules));
    }
    
    /**
     * 지연 보상 마일리지 조회
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/delay-compensation")
    @Operation(summary = "지연 보상 마일리지 조회", description = "열차 지연으로 인한 보상 마일리지를 조회합니다")
    public ResponseEntity<SuccessResponse<List<MileageTransaction>>> getDelayCompensation(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("지연 보상 마일리지 조회 - 회원번호: {}", userDetails.getUsername());
        
        List<MileageTransaction> compensations = 
                mileageTransactionService.getDelayCompensationTransactions(userDetails);
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_TRANSACTION_INQUIRY_SUCCESS, compensations));
    }
    
    /**
     * 마일리지 적립 이력 조회 (Train별)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping("/earning-history")
    @Operation(summary = "마일리지 적립 이력 조회", description = "특정 기차별 마일리지 적립 이력을 조회합니다")
    public ResponseEntity<SuccessResponse<List<MileageTransaction>>> getEarningHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "기차 ID (선택사항)")
            @RequestParam(required = false) String trainId,
            
            @Parameter(description = "조회 시작일 (선택사항)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일 (선택사항)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.debug("마일리지 적립 이력 조회 - 회원번호: {}, 기차ID: {}", userDetails.getUsername(), trainId);
        
        List<MileageTransaction> history = 
                mileageTransactionService.getEarningHistory(userDetails, trainId, startDate, endDate);
        
        return ResponseEntity.ok(SuccessResponse.of(PaymentSuccess.MILEAGE_TRANSACTION_INQUIRY_SUCCESS, history));
    }
    
    // Response DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class MileageBalanceResponse {
        private Long memberId;
        private BigDecimal currentBalance;
        private BigDecimal activeBalance;
        private BigDecimal pendingEarning;
        private BigDecimal expiringInMonth;
        private LocalDateTime lastUpdatedAt;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SimpleMileageBalanceResponse {
        private BigDecimal balance;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class AvailableMileageResponse {
        private BigDecimal availableBalance;
        private BigDecimal minimumUsableAmount;
        private Integer maximumUsablePercentage;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class MileageTransactionListResponse {
        private List<MileageTransaction> transactions;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}