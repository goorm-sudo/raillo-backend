package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.context.PaymentContext;
import com.sudo.railo.payment.application.dto.request.PaymentExecuteRequest;
import com.sudo.railo.payment.application.dto.response.PaymentCalculationResponse;
import com.sudo.railo.payment.application.port.out.LoadPaymentPort;
import com.sudo.railo.payment.application.port.out.SavePaymentPort;
import com.sudo.railo.payment.application.port.out.LoadMemberPort;
import com.sudo.railo.payment.domain.entity.CashReceipt;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.entity.PaymentExecutionStatus;
import com.sudo.railo.payment.domain.entity.PaymentMethod;
import com.sudo.railo.payment.domain.entity.PaymentCalculation;
import com.sudo.railo.booking.infra.JpaReservationRepository;
import com.sudo.railo.train.application.TrainScheduleService;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.train.domain.type.TrainOperator;
import com.sudo.railo.payment.domain.repository.PaymentCalculationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sudo.railo.payment.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 생성 전용 서비스
 * 
 * PaymentContext를 기반으로 Payment 엔티티를 생성하고 저장
 * 중복 결제 방지 및 비회원 정보 처리 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCreationService {
    
    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final LoadMemberPort loadMemberPort;
    private final PasswordEncoder passwordEncoder;
    
    // 열차 정보를 위한 의존성 추가
    private final JpaReservationRepository reservationRepository;
    private final TrainScheduleService trainScheduleService;
    private final PaymentCalculationRepository calculationRepository;
    
    /**
     * Payment 엔티티 생성 및 저장
     * 
     * @param context 검증된 결제 컨텍스트
     * @return 저장된 Payment 엔티티
     */
    @Transactional
    public Payment createPayment(PaymentContext context) {
        log.info("결제 생성 시작 - idempotencyKey: {}", context.getIdempotencyKey());
        
        // 1. 중복 결제 체크 (멱등성 보장)
        validateIdempotency(context.getIdempotencyKey());
        
        // 2. Payment 엔티티 생성
        Payment payment = buildPayment(context);
        
        // 3. 비회원 정보는 이미 builder에서 처리됨
        
        // 4. 저장
        Payment savedPayment = savePaymentPort.save(payment);
        log.info("결제 엔티티 생성 완료 - paymentId: {}", savedPayment.getId());
        
        return savedPayment;
    }
    
    /**
     * 멱등성 검증 - 중복 결제 방지
     */
    private void validateIdempotency(String idempotencyKey) {
        if (loadPaymentPort.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("중복 결제 요청 감지 - idempotencyKey: {}", idempotencyKey);
            throw new PaymentValidationException("이미 처리된 결제 요청입니다");
        }
    }
    
    /**
     * Payment 엔티티 빌드
     */
    private Payment buildPayment(PaymentContext context) {
        // reservationId 파싱 (문자열 형태인 경우 처리)
        Long reservationId = parseReservationId(context.getCalculation().getReservationId());
        log.info("🎯 Payment 생성 - calculationId: {}, reservationId: {}, externalOrderId: {}", 
            context.getCalculation().getId(), reservationId, context.getCalculation().getExternalOrderId());
        
        // 마일리지 정보 추출
        BigDecimal mileagePointsUsed = BigDecimal.ZERO;
        BigDecimal mileageAmountDeducted = BigDecimal.ZERO;
        if (context.hasMileageUsage()) {
            mileagePointsUsed = context.getMileageResult().getUsageAmount();
            mileageAmountDeducted = context.getMileageResult().getUsageAmount(); // 1포인트 = 1원 고정
        }
        
        // 최종 결제 금액 (이미 마일리지가 차감된 금액)
        BigDecimal finalPayableAmount = context.getFinalPayableAmount();
        
        // 마일리지 적립 예정 금액 계산 (회원인 경우만)
        BigDecimal mileageToEarn = BigDecimal.ZERO;
        if (context.isForMember()) {
            // 1% 적립 (추후 정책에 따라 변경 가능)
            mileageToEarn = finalPayableAmount.multiply(BigDecimal.valueOf(0.01))
                .setScale(0, java.math.RoundingMode.DOWN);
        }
        
        // 열차 정보 조회 (예약이 삭제되어도 환불 가능하도록)
        Long trainScheduleId = null;
        java.time.LocalDateTime trainDepartureTime = null;
        java.time.LocalDateTime trainArrivalTime = null;
        TrainOperator trainOperator = null;
        
        // 1차: 예약에서 열차 정보 조회 시도 (reservationId가 있는 경우만)
        if (reservationId != null) {
            try {
                Reservation reservation = reservationRepository.findById(reservationId)
                    .orElse(null);
                if (reservation != null && reservation.getTrainSchedule() != null) {
                    trainScheduleId = reservation.getTrainSchedule().getId();
                    
                    // TrainScheduleService에서 시간 정보 가져오기
                    TrainScheduleService.TrainTimeInfo timeInfo = 
                        trainScheduleService.getTrainTimeInfo(trainScheduleId);
                    if (timeInfo != null) {
                        trainDepartureTime = timeInfo.departureTime();
                        trainArrivalTime = timeInfo.actualArrivalTime() != null 
                            ? timeInfo.actualArrivalTime() 
                            : timeInfo.scheduledArrivalTime();
                    }
                    
                    // 운영사 정보 추출
                    String trainName = reservation.getTrainSchedule().getTrain().getTrainName();
                    if (trainName != null) {
                        if (trainName.contains("KTX") || trainName.contains("산천") || 
                            trainName.contains("ITX") || trainName.contains("새마을")) {
                            trainOperator = TrainOperator.KORAIL;
                        } else if (trainName.contains("SRT")) {
                            trainOperator = TrainOperator.SRT;
                        } else {
                            trainOperator = TrainOperator.KORAIL; // 기본값
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("예약에서 열차 정보 조회 실패 - reservationId: {}, error: {}", 
                    reservationId, e.getMessage());
            }
        }
        
        // 2차: 예약 조회 실패 시 PaymentCalculation에서 열차 정보 가져오기
        if (trainScheduleId == null && context.getCalculation() != null) {
            PaymentCalculationResponse calculation = context.getCalculation();
            try {
                PaymentCalculation calcEntity = 
                    calculationRepository.findById(calculation.getId()).orElse(null);
                if (calcEntity != null) {
                    trainScheduleId = calcEntity.getTrainScheduleId();
                    trainDepartureTime = calcEntity.getTrainDepartureTime();
                    trainArrivalTime = calcEntity.getTrainArrivalTime();
                    trainOperator = calcEntity.getTrainOperator();
                    log.info("예약 조회 실패, PaymentCalculation에서 열차 정보 사용 - trainScheduleId: {}", 
                        trainScheduleId);
                }
            } catch (Exception e) {
                log.warn("PaymentCalculation에서 열차 정보 조회 실패 - calculationId: {}, error: {}", 
                    calculation.getId(), e.getMessage());
            }
        }
        
        Payment.PaymentBuilder builder = Payment.builder()
            .reservationId(reservationId)
            .externalOrderId(context.getCalculation().getExternalOrderId())
            .amountOriginalTotal(context.getCalculation().getOriginalAmount())
            .totalDiscountAmountApplied(context.getCalculation().getTotalDiscountAmount())
            .mileagePointsUsed(mileagePointsUsed)
            .mileageAmountDeducted(mileageAmountDeducted)
            .amountPaid(finalPayableAmount)
            .mileageToEarn(mileageToEarn)
            .paymentMethod(PaymentMethod.valueOf(
                context.getRequest().getPaymentMethod().getType()))
            .pgProvider(context.getRequest().getPaymentMethod().getPgProvider())
            .paymentStatus(PaymentExecutionStatus.PENDING)
            .idempotencyKey(context.getIdempotencyKey())
            .trainScheduleId(trainScheduleId)
            .trainDepartureTime(trainDepartureTime)
            .trainArrivalTime(trainArrivalTime)
            .trainOperator(trainOperator);
        
        // 회원/비회원별 추가 정보 설정
        if (context.isForMember()) {
            // PaymentContext에서 회원 ID 가져오기
            Long memberId = context.getMemberId();
            if (memberId == null) {
                throw new PaymentValidationException("회원 결제에 회원 ID가 없습니다");
            }
            
            // 회원 엔티티 조회
            com.sudo.railo.member.domain.Member member = loadMemberPort.findById(memberId)
                .orElseThrow(() -> new PaymentValidationException("회원 정보를 찾을 수 없습니다: " + memberId));
            
            // Payment 엔티티에 Member 설정
            builder.member(member);
            
            log.debug("회원 결제 설정 - memberId: {}", memberId);
        } else {
            // 비회원 정보 검증 및 암호화
            validateNonMemberInfo(context.getRequest());
            String encodedPassword = passwordEncoder.encode(context.getRequest().getNonMemberPassword());
            
            builder.nonMemberName(context.getRequest().getNonMemberName().trim())
                   .nonMemberPhone(normalizePhoneNumber(context.getRequest().getNonMemberPhone()))
                   .nonMemberPassword(encodedPassword);
            log.debug("비회원 결제 설정 - name: {}", context.getRequest().getNonMemberName());
        }
        
        // 현금영수증 정보 설정
        CashReceipt cashReceipt = buildCashReceipt(context);
        if (cashReceipt != null) {
            builder.cashReceipt(cashReceipt);
        }
        
        return builder.build();
    }
    
    /**
     * 예약 ID 파싱 - Optional 처리
     */
    private Long parseReservationId(String reservationIdStr) {
        log.info("🔍 예약 ID 파싱 시도 - 입력값: '{}', null여부: {}, 'null'문자열여부: {}", 
            reservationIdStr, 
            reservationIdStr == null, 
            "null".equals(reservationIdStr));
        
        // null 또는 "null" 문자열 체크 - 이제 null 허용
        if (reservationIdStr == null || "null".equals(reservationIdStr) || reservationIdStr.trim().isEmpty()) {
            log.info("⚠️ 예약 ID가 없습니다. 열차 정보는 PaymentCalculation에서 가져옵니다.");
            return null; // null 반환 허용
        }
        
        try {
            // 'R' 접두사 제거 (있는 경우)
            String cleanId = reservationIdStr.startsWith("R") ? 
                reservationIdStr.substring(1) : reservationIdStr;
            
            Long reservationId = Long.parseLong(cleanId);
            log.info("✅ 예약 ID 파싱 성공 - 원본: '{}', 파싱결과: {}", reservationIdStr, reservationId);
            return reservationId;
        } catch (NumberFormatException e) {
            log.error("❌ 예약 ID 파싱 실패 - 입력값: '{}', 오류: {}", reservationIdStr, e.getMessage());
            throw new PaymentValidationException(
                "잘못된 예약 ID 형식입니다: " + reservationIdStr);
        }
    }
    
    /**
     * 현금영수증 정보 생성
     */
    private CashReceipt buildCashReceipt(PaymentContext context) {
        var cashReceiptInfo = context.getRequest().getCashReceiptInfo();
        
        if (cashReceiptInfo == null || !cashReceiptInfo.isRequested()) {
            return CashReceipt.notRequested();
        }
        
        if ("personal".equals(cashReceiptInfo.getType())) {
            return CashReceipt.createPersonalReceipt(cashReceiptInfo.getPhoneNumber());
        } else if ("business".equals(cashReceiptInfo.getType())) {
            return CashReceipt.createBusinessReceipt(cashReceiptInfo.getBusinessNumber());
        }
        
        return CashReceipt.notRequested();
    }
    
    /**
     * 비회원 정보 검증
     */
    private void validateNonMemberInfo(PaymentExecuteRequest request) {
        if (request.getNonMemberName() == null || request.getNonMemberName().trim().isEmpty()) {
            throw new PaymentValidationException("비회원 이름은 필수입니다");
        }
        
        if (request.getNonMemberPhone() == null || request.getNonMemberPhone().trim().isEmpty()) {
            throw new PaymentValidationException("비회원 전화번호는 필수입니다");
        }
        
        if (request.getNonMemberPassword() == null || !request.getNonMemberPassword().matches("^[0-9]{5}$")) {
            throw new PaymentValidationException("비회원 비밀번호는 5자리 숫자여야 합니다");
        }
        
        // 전화번호 형식 검증
        String cleanedPhone = request.getNonMemberPhone().replaceAll("[^0-9]", "");
        if (!cleanedPhone.matches("^01[016789]\\d{7,8}$")) {
            throw new PaymentValidationException("올바른 전화번호 형식이 아닙니다");
        }
    }
    
    /**
     * 전화번호 정규화
     */
    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }
}