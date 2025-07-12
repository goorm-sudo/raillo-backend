package com.sudo.railo.payment.infrastructure.client;

import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.infrastructure.client.dto.PgVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mock PG API 클라이언트
 * 개발/테스트 환경에서 사용
 * 
 * TODO: 운영 환경에서는 실제 PG사 API 연동 구현 필요
 * - 실제 API 인증 토큰 관리
 * - API 호출 및 서명 검증
 * - 에러 처리 및 재시도 로직
 * - 타임아웃 처리
 */
@Profile("!prod")
@Component
@RequiredArgsConstructor
@Slf4j
public class MockPgApiClient implements PgApiClient {
    
    private final PaymentCalculationRepository calculationRepository;
    
    @Override
    public PgVerificationResult verifyPayment(String authNumber, String orderId) {
        log.warn("⚠️ Mock PG API 사용 중 - 운영 환경에서는 실제 PG 연동 필요");
        log.info("Mock PG 검증 요청: authNumber={}, orderId={}", authNumber, orderId);
        
        // Mock 승인번호 검증 (형식만 체크)
        if (!authNumber.startsWith("MOCK-")) {
            throw new PaymentValidationException("Mock 환경에서는 MOCK- 접두사 필요");
        }
        
        // 실제처럼 동작하기 위해 계산 세션에서 금액 조회
        PaymentCalculation calculation = calculationRepository.findByPgOrderId(orderId)
            .orElseThrow(() -> new PaymentValidationException("유효하지 않은 주문번호"));
        
        // Mock 응답 생성 (항상 승인)
        // TODO: 실제 PG 연동 시 아래 로직을 실제 API 호출로 변경
        PgVerificationResult result = PgVerificationResult.builder()
            .success(true)
            .amount(calculation.getFinalAmount()) // 계산된 금액 그대로 반환
            .authNumber(authNumber)
            .approvedAt(LocalDateTime.now())
            .cardNumber("****-****-****-1234") // Mock 카드번호
            .cardType("신용카드")
            .build();
        
        log.info("Mock PG 검증 성공: amount={}, authNumber={}", 
            result.getAmount(), result.getAuthNumber());
        
        return result;
    }
}