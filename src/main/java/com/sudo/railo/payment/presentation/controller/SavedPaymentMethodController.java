package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.CreateSavedPaymentMethodRequest;
import com.sudo.railo.payment.application.dto.SavedPaymentMethodRequestDto;
import com.sudo.railo.payment.application.dto.SavedPaymentMethodResponseDto;
import com.sudo.railo.payment.application.service.SavedPaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * 저장된 결제수단 관리 API 컨트롤러
 * 
 * 보안이 강화된 결제수단 저장/조회 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/saved-payment-methods")
@RequiredArgsConstructor
@Tag(name = "SavedPaymentMethod", description = "저장된 결제수단 관리 API")
public class SavedPaymentMethodController {

    private final SavedPaymentMethodService savedPaymentMethodService;

    /**
     * 새로운 결제수단 저장
     * JWT 토큰에서 memberId를 자동으로 추출하여 요청 DTO에 설정
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결제수단 저장", description = "새로운 결제수단을 암호화하여 저장합니다.")
    public ResponseEntity<SavedPaymentMethodResponseDto> savePaymentMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSavedPaymentMethodRequest request) {
        
        // CreateSavedPaymentMethodRequest를 SavedPaymentMethodRequestDto로 변환
        SavedPaymentMethodRequestDto dto = SavedPaymentMethodRequestDto.builder()
                .memberId(null) // Service에서 UserDetails로 memberId 설정
                .paymentMethodType(request.getPaymentMethodType())
                .alias(request.getAlias())
                .cardNumber(request.getCardNumber())
                .cardHolderName(request.getCardHolderName())
                .cardExpiryMonth(request.getCardExpiryMonth())
                .cardExpiryYear(request.getCardExpiryYear())
                .cardCvc(request.getCardCvc())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .accountPassword(request.getAccountPassword())
                .isDefault(request.getIsDefault())
                .build();
        
        log.info("Save payment method request for memberNo: {}", userDetails.getUsername());
        SavedPaymentMethodResponseDto response = savedPaymentMethodService.savePaymentMethod(dto, userDetails);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 회원의 저장된 결제수단 목록 조회 (마스킹된 정보)
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결제수단 목록 조회", description = "회원의 저장된 결제수단 목록을 조회합니다. 민감정보는 마스킹됩니다.")
    public ResponseEntity<List<SavedPaymentMethodResponseDto>> getPaymentMethods(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Get payment methods for memberNo: {}", userDetails.getUsername());
        List<SavedPaymentMethodResponseDto> methods = savedPaymentMethodService.getPaymentMethods(userDetails);
        
        return ResponseEntity.ok(methods);
    }

    /**
     * 결제 실행을 위한 결제수단 상세 조회 (원본 정보)
     * JWT 토큰에서 memberId를 자동으로 추출
     * 실제 결제 실행 시에만 사용하며, 특별한 권한 검증과 감사 로그를 남김
     */
    @GetMapping("/{id}/raw")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결제수단 상세 조회 (결제용)", 
              description = "실제 결제를 위한 결제수단 정보를 조회합니다. 복호화된 원본 정보가 반환됩니다.")
    public ResponseEntity<SavedPaymentMethodResponseDto> getPaymentMethodForPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "결제수단 ID") @PathVariable Long id,
            @Parameter(description = "결제 세션 ID") @RequestParam(required = false) String sessionId) {
        
        log.info("Get payment method for payment - ID: {}, MemberNo: {}, Session: {}", id, userDetails.getUsername(), sessionId);
        SavedPaymentMethodResponseDto method = savedPaymentMethodService.getPaymentMethodForPayment(id, userDetails);
        
        return ResponseEntity.ok(method);
    }

    /**
     * 결제수단 삭제
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "결제수단 삭제", description = "저장된 결제수단을 삭제합니다.")
    public ResponseEntity<Void> deletePaymentMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "결제수단 ID") @PathVariable Long id) {
        
        log.info("Delete payment method - ID: {}, MemberNo: {}", id, userDetails.getUsername());
        savedPaymentMethodService.deletePaymentMethod(id, userDetails);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 기본 결제수단 설정
     * JWT 토큰에서 memberId를 자동으로 추출
     */
    @PutMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "기본 결제수단 설정", description = "특정 결제수단을 기본 결제수단으로 설정합니다.")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "결제수단 ID") @PathVariable Long id) {
        
        log.info("Set default payment method - ID: {}, MemberNo: {}", id, userDetails.getUsername());
        savedPaymentMethodService.setDefaultPaymentMethod(id, userDetails);
        
        return ResponseEntity.ok().build();
    }
}