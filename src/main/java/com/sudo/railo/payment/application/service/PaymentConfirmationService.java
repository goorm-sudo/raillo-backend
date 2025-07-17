package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.CalculationStatus;
import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.domain.entity.MemberType;
import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.repository.MileageEarningScheduleRepository;
import com.sudo.railo.payment.exception.PaymentValidationException;
import com.sudo.railo.payment.exception.DuplicatePgAuthException;
import com.sudo.railo.payment.infrastructure.client.PgApiClient;
import com.sudo.railo.payment.infrastructure.client.dto.PgVerificationResult;
import com.sudo.railo.payment.interfaces.dto.request.PaymentConfirmRequest;
import com.sudo.railo.payment.interfaces.dto.response.PaymentResponse;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import com.sudo.railo.payment.domain.service.MileageExecutionService;
import com.sudo.railo.payment.application.dto.PaymentResult.MileageExecutionResult;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 결제 확인 서비스
 * PG 결제 후 최종 확인 및 검증 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConfirmationService {
    
    private final PaymentCalculationRepository calculationRepository;
    private final PaymentRepository paymentRepository;
    private final MileageEarningScheduleRepository mileageEarningScheduleRepository;
    private final PgApiClient pgApiClient;
    private final PasswordEncoder passwordEncoder;
    private final PaymentEventPublisher paymentEventPublisher;
    private final MileageExecutionService mileageExecutionService;
    private final MemberRepository memberRepository;
    
    /**
     * PG 결제 확인 및 최종 처리
     * 
     * @param request 결제 확인 요청 (calculationId + pgAuthNumber)
     * @return 결제 응답
     */
    @Transactional
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        log.info("결제 확인 시작: calculationId={}, pgAuthNumber={}", 
            request.getCalculationId(), request.getPgAuthNumber());
        
        // 1. 계산 세션 조회 및 검증
        PaymentCalculation calculation = getAndValidateCalculation(request.getCalculationId());
        
        // 2. PG 승인번호 중복 확인
        validatePgAuthNumber(request.getPgAuthNumber());
        
        // 3. PG사 API로 직접 검증
        PgVerificationResult pgResult = verifyWithPg(
            request.getPgAuthNumber(), 
            calculation.getPgOrderId()
        );
        
        // 4. 금액 일치 확인
        validateAmount(calculation.getFinalAmount(), pgResult.getAmount(), 
                      calculation.getId(), calculation.getPgOrderId());
        
        // 5. 결제 생성 및 완료 처리
        Payment payment = createPayment(calculation, request, pgResult);
        
        // 6. 계산 세션 사용 처리
        markCalculationAsUsed(calculation);
        
        // 7. 마일리지 차감 (있는 경우)
        processMileageUsage(calculation);
        
        // 8. 마일리지 적립 스케줄은 이벤트 리스너에서 처리 (중복 방지)
        // createMileageEarningSchedule(payment, calculation);
        
        log.info("결제 확인 완료: paymentId={}", payment.getId());
        
        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .status("SUCCESS")
            .amount(payment.getAmountPaid())
            .paymentMethod(payment.getPaymentMethod().name())
            .completedAt(payment.getUpdatedAt())
            .pgTransactionId(payment.getPgTransactionId())
            .pgApprovalNumber(payment.getPgApprovalNo())
            .build();
    }
    
    /**
     * 계산 세션 조회 및 검증
     */
    private PaymentCalculation getAndValidateCalculation(String calculationId) {
        PaymentCalculation calculation = calculationRepository.findById(calculationId)
            .orElseThrow(() -> new PaymentValidationException("유효하지 않은 계산 세션입니다"));
        
        // 상태 확인
        if (calculation.getStatus() != CalculationStatus.CALCULATED) {
            if (calculation.getStatus() == CalculationStatus.USED || 
                calculation.getStatus() == CalculationStatus.CONSUMED) {
                throw new PaymentValidationException("이미 사용된 계산 세션입니다");
            }
            throw new PaymentValidationException("유효하지 않은 계산 세션 상태입니다");
        }
        
        // 만료 확인
        if (calculation.isExpired()) {
            calculation.markAsExpired();
            calculationRepository.save(calculation);
            throw new PaymentValidationException("계산 세션이 만료되었습니다");
        }
        
        return calculation;
    }
    
    /**
     * PG 승인번호 중복 확인
     */
    private void validatePgAuthNumber(String pgAuthNumber) {
        // Repository에 existsByPgApprovalNo 메서드가 없으므로 findByPgApprovalNo로 대체
        if (paymentRepository.findByPgApprovalNo(pgAuthNumber).isPresent()) {
            log.error("PG 승인번호 중복 사용 시도: {}", pgAuthNumber);
            throw new DuplicatePgAuthException("이미 사용된 승인번호입니다");
        }
    }
    
    /**
     * PG사 API로 검증
     */
    private PgVerificationResult verifyWithPg(String authNumber, String pgOrderId) {
        try {
            PgVerificationResult result = pgApiClient.verifyPayment(authNumber, pgOrderId);
            
            if (!result.isSuccess()) {
                throw new PaymentValidationException("PG 승인 검증 실패: " + result.getMessage());
            }
            
            return result;
        } catch (Exception e) {
            log.error("PG 검증 중 오류 발생", e);
            throw new PaymentValidationException("PG 검증 처리 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 금액 일치 확인
     */
    private void validateAmount(java.math.BigDecimal calculatedAmount, java.math.BigDecimal pgAmount,
                               String calculationId, String pgOrderId) {
        if (calculatedAmount.compareTo(pgAmount) != 0) {
            log.error("결제 금액 불일치: calculated={}, pg={}", calculatedAmount, pgAmount);
            
            // 금액 불일치 알림 이벤트 발행
            paymentEventPublisher.publishAmountMismatchAlert(
                calculationId,
                calculatedAmount,
                pgAmount,
                pgOrderId
            );
            
            throw new PaymentValidationException("결제 금액이 일치하지 않습니다");
        }
    }
    
    /**
     * 결제 생성
     */
    private Payment createPayment(PaymentCalculation calculation, PaymentConfirmRequest request, 
                                  PgVerificationResult pgResult) {
        
        // Payment 엔티티 빌더 사용
        Payment.PaymentBuilder paymentBuilder = Payment.builder()
            .reservationId(calculation.getReservationId() != null ? 
                Long.parseLong(calculation.getReservationId()) : null)
            .externalOrderId(calculation.getExternalOrderId())
            .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
            .amountOriginalTotal(calculation.getOriginalAmount())
            .amountPaid(calculation.getFinalAmount())
            .paymentStatus(PaymentExecutionStatus.SUCCESS)
            .pgTransactionId(pgResult.getAuthNumber())
            .pgApprovalNo(pgResult.getAuthNumber())
            .paidAt(LocalDateTime.now())
            .idempotencyKey(calculation.getId()); // 계산 ID를 멱등성 키로 사용
        
        // 회원 설정 (회원인 경우)
        if (determineMemberType(calculation.getUserIdExternal()) == MemberType.MEMBER) {
            try {
                Long memberId = Long.parseLong(calculation.getUserIdExternal());
                Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new PaymentValidationException(
                        String.format("회원 정보를 찾을 수 없습니다. memberId: %d", memberId)));
                paymentBuilder.member(member);
                log.debug("회원 결제 설정 완료: memberId={}", memberId);
            } catch (NumberFormatException e) {
                throw new PaymentValidationException(
                    String.format("잘못된 회원 ID 형식입니다: %s", calculation.getUserIdExternal()));
            }
        }
        
        // 비회원인 경우 추가 정보 설정
        if (determineMemberType(calculation.getUserIdExternal()) == MemberType.NON_MEMBER) {
            paymentBuilder
                .nonMemberName(request.getNonMemberName())
                .nonMemberPhone(request.getNonMemberPhone())
                .nonMemberPassword(request.getNonMemberPassword() != null ? 
                    passwordEncoder.encode(request.getNonMemberPassword()) : null);
        }
        
        // 열차 정보 설정
        paymentBuilder
            .trainScheduleId(calculation.getTrainScheduleId())
            .trainDepartureTime(calculation.getTrainDepartureTime())
            .trainArrivalTime(calculation.getTrainArrivalTime())
            // .trainOperator(calculation.getTrainOperator()); // 제거됨
            ;
        
        // 마일리지 정보 설정
        if (calculation.getMileageToUse() != null && 
            calculation.getMileageToUse().compareTo(java.math.BigDecimal.ZERO) > 0) {
            paymentBuilder
                .mileagePointsUsed(calculation.getMileageToUse())
                .mileageAmountDeducted(calculation.getMileageDiscount());
        }
        
        Payment payment = paymentBuilder.build();
        return paymentRepository.save(payment);
    }
    
    /**
     * 계산 세션을 사용됨으로 표시
     */
    private void markCalculationAsUsed(PaymentCalculation calculation) {
        calculation.markAsUsed();
        calculationRepository.save(calculation);
    }
    
    /**
     * 마일리지 차감 처리
     */
    private void processMileageUsage(PaymentCalculation calculation) {
        if (calculation.getMileageToUse() != null && 
            calculation.getMileageToUse().compareTo(java.math.BigDecimal.ZERO) > 0) {
            
            // 회원 여부 확인
            if (determineMemberType(calculation.getUserIdExternal()) != MemberType.MEMBER) {
                log.info("비회원 결제로 마일리지 차감 건너뛰기: userId={}", calculation.getUserIdExternal());
                return;
            }
            
            try {
                Long memberId = Long.parseLong(calculation.getUserIdExternal());
                
                // 마일리지 차감은 Payment 객체 생성 후 PaymentExecutionService에서 처리됨
                // 여기서는 계산 세션에 마일리지 사용 정보만 기록
                log.info("마일리지 차감 예정: memberId={}, amount={}", memberId, calculation.getMileageToUse());
                
                // 실제 차감은 PaymentConfirmRequest를 통해 Payment 생성 후 
                // PaymentExecutionService.execute()에서 mileageExecutionService.executeUsage(payment) 호출로 처리
                
            } catch (NumberFormatException e) {
                throw new PaymentValidationException(
                    String.format("잘못된 회원 ID 형식입니다: %s", calculation.getUserIdExternal()));
            }
        }
    }
    
    /**
     * 회원 타입 판단
     */
    private MemberType determineMemberType(String userId) {
        return "guest_user".equals(userId) ? MemberType.NON_MEMBER : MemberType.MEMBER;
    }
    
    /**
     * 마일리지 적립 스케줄 생성
     * 열차 도착 시점에 마일리지가 적립되도록 스케줄 생성
     * @deprecated 이벤트 리스너에서 처리하도록 변경 (중복 방지)
     */
    @Deprecated
    private void createMileageEarningSchedule(Payment payment, PaymentCalculation calculation) {
        log.info("마일리지 적립 스케줄 생성 시작 - paymentId: {}, memberId: {}", 
            payment.getId(), payment.getMemberId());
        
        // 회원 결제인 경우에만 마일리지 적립
        if (payment.getMemberId() == null) {
            log.info("비회원 결제로 마일리지 적립 스케줄 생성 건너뛰기 - paymentId: {}", payment.getId());
            return;
        }
        
        // 열차 정보가 없으면 스케줄 생성 불가
        if (calculation.getTrainArrivalTime() == null || calculation.getTrainScheduleId() == null) {
            log.warn("열차 정보 부족으로 마일리지 적립 스케줄 생성 불가 - paymentId: {}, trainScheduleId: {}, trainArrivalTime: {}", 
                payment.getId(), calculation.getTrainScheduleId(), calculation.getTrainArrivalTime());
            return;
        }
        
        try {
            // 노선 정보 생성 (기본값 사용)
            // TODO: PaymentCalculation에 출발역/도착역 정보 추가 필요
            String routeInfo = "서울-부산"; // 임시로 고정값 사용
            
            // 열차 도착 시점에 적립되도록 스케줄 생성
            MileageEarningSchedule schedule = MileageEarningSchedule.createNormalEarningSchedule(
                calculation.getTrainScheduleId(),
                payment.getId().toString(),
                payment.getMemberId(),
                payment.getAmountPaid(),
                calculation.getTrainArrivalTime(), // 도착 시점에 적립
                routeInfo
            );
            
            mileageEarningScheduleRepository.save(schedule);
            
            log.info("마일리지 적립 스케줄 생성 완료 - scheduleId: {}, paymentId: {}, memberId: {}, scheduledTime: {}", 
                schedule.getId(), payment.getId(), payment.getMemberId(), calculation.getTrainArrivalTime());
                
        } catch (Exception e) {
            // 마일리지 적립 스케줄 생성 실패가 결제를 실패시키지 않도록 함
            log.error("마일리지 적립 스케줄 생성 실패 - paymentId: {}", payment.getId(), e);
        }
    }
}