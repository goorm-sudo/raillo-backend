package com.sudo.railo.payment.presentation.controller;

import com.sudo.railo.payment.application.dto.request.BankAccountVerificationRequest;
import com.sudo.railo.payment.application.dto.response.BankAccountVerificationResponse;
import com.sudo.railo.payment.application.service.BankAccountVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 은행 계좌 검증 전용 컨트롤러
 * 계좌 저장 없이 유효성만 검증
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment/verify-bank-account")
@RequiredArgsConstructor
@Tag(name = "BankAccountVerification", description = "은행 계좌 검증 API")
public class BankAccountVerificationController {

    private final BankAccountVerificationService verificationService;

    /**
     * 은행 계좌 유효성 검증
     * 계좌번호와 비밀번호를 검증하고 예금주명을 반환
     * 검증만 수행하고 저장하지 않음
     */
    @PostMapping
    @Operation(summary = "계좌 검증", description = "은행 계좌의 유효성을 검증합니다. 저장하지 않고 검증만 수행합니다.")
    public ResponseEntity<BankAccountVerificationResponse> verifyBankAccount(
            @Valid @RequestBody BankAccountVerificationRequest request) {
        
        log.info("계좌 검증 요청 - 은행: {}, 계좌번호: {}", 
                request.getBankCode(), 
                request.getAccountNumber().replaceAll("(\\d{4})(\\d+)(\\d{4})", "$1****$3"));
        
        BankAccountVerificationResponse response = verificationService.verifyAccount(request);
        
        return ResponseEntity.ok(response);
    }
}