package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.response.MileageBalanceInfo;
import com.sudo.railo.payment.application.dto.response.MileageStatistics;
import com.sudo.railo.payment.application.service.MileageBalanceService;
import com.sudo.railo.payment.domain.entity.MileageTransaction;
import com.sudo.railo.payment.success.MileageSuccess;
import com.sudo.railo.global.success.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마일리지 관련 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/mileage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "마일리지 관리", description = "마일리지 잔액 조회, 통계, 사용 가능 내역 등 관련 API")
public class MileageController {
    
    private final MileageBalanceService mileageBalanceService;
    
    /**
     * 회원 마일리지 잔액 조회
     */
    @GetMapping("/balance/{memberId}")
    @Operation(summary = "마일리지 잔액 조회", description = "회원의 현재 마일리지 잔액과 통계 정보를 조회합니다")
    public ResponseEntity<SuccessResponse<MileageBalanceInfo>> getMileageBalance(
            @Parameter(description = "회원 ID", required = true)
            @PathVariable @NotNull Long memberId) {
        
        log.info("마일리지 잔액 조회 API 호출 - 회원ID: {}", memberId);
        
        MileageBalanceInfo balanceInfo = mileageBalanceService.getMileageBalance(memberId);
        
        return ResponseEntity.ok(
                SuccessResponse.of(MileageSuccess.MILEAGE_BALANCE_SUCCESS, balanceInfo)
        );
    }
    
    /**
     * 사용 가능한 마일리지 내역 조회 (FIFO 순서)
     */
    @GetMapping("/available/{memberId}")
    @Operation(summary = "사용 가능한 마일리지 조회", description = "회원의 사용 가능한 마일리지를 만료일 순서로 조회합니다")
    public ResponseEntity<SuccessResponse<List<MileageTransaction>>> getAvailableMileage(
            @Parameter(description = "회원 ID", required = true)
            @PathVariable @NotNull Long memberId) {
        
        log.info("사용 가능한 마일리지 조회 API 호출 - 회원ID: {}", memberId);
        
        List<MileageTransaction> availableMileage = mileageBalanceService.getAvailableMileageForUsage(memberId);
        
        return ResponseEntity.ok(
                SuccessResponse.of(MileageSuccess.MILEAGE_AVAILABLE_SUCCESS, availableMileage)
        );
    }
    
    /**
     * 기간별 마일리지 통계 조회
     */
    @GetMapping("/statistics/{memberId}")
    @Operation(summary = "기간별 마일리지 통계", description = "특정 기간의 마일리지 적립/사용 통계를 조회합니다")
    public ResponseEntity<SuccessResponse<MileageStatistics>> getMileageStatistics(
            @Parameter(description = "회원 ID", required = true)
            @PathVariable @NotNull Long memberId,
            
            @Parameter(description = "조회 시작일 (ISO 형식)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "조회 종료일 (ISO 형식)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("마일리지 통계 조회 API 호출 - 회원ID: {}, 기간: {} ~ {}", memberId, startDate, endDate);
        
        MileageStatistics statistics = mileageBalanceService.getMileageStatisticsByPeriod(memberId, startDate, endDate);
        
        return ResponseEntity.ok(
                SuccessResponse.of(MileageSuccess.MILEAGE_STATISTICS_SUCCESS, statistics)
        );
    }
    
    /**
     * 마일리지 잔액 간단 조회 (잔액만)
     */
    @GetMapping("/balance/{memberId}/simple")
    @Operation(summary = "마일리지 잔액 간단 조회", description = "회원의 현재 마일리지 잔액만 간단히 조회합니다")
    public ResponseEntity<SuccessResponse<SimpleMileageBalance>> getSimpleMileageBalance(
            @Parameter(description = "회원 ID", required = true)
            @PathVariable @NotNull Long memberId) {
        
        log.info("마일리지 간단 잔액 조회 API 호출 - 회원ID: {}", memberId);
        
        MileageBalanceInfo balanceInfo = mileageBalanceService.getMileageBalance(memberId);
        
        SimpleMileageBalance simpleBalance = SimpleMileageBalance.builder()
                .memberId(memberId)
                .currentBalance(balanceInfo.getCurrentBalance())
                .activeBalance(balanceInfo.getActiveBalance())
                .expiringMileage(balanceInfo.getExpiringMileage())
                .build();
        
        return ResponseEntity.ok(
                SuccessResponse.of(MileageSuccess.MILEAGE_BALANCE_SUCCESS, simpleBalance)
        );
    }
    
    /**
     * 간단한 마일리지 잔액 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SimpleMileageBalance {
        private Long memberId;
        private java.math.BigDecimal currentBalance;    // 현재 총 잔액
        private java.math.BigDecimal activeBalance;     // 활성 잔액
        private java.math.BigDecimal expiringMileage;   // 만료 예정 마일리지
    }
} 