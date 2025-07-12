package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.*;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.domain.repository.RefundCalculationRepository;
import com.sudo.railo.payment.domain.repository.MileageTransactionRepository;
import com.sudo.railo.payment.domain.repository.MileageEarningScheduleRepository;
import com.sudo.railo.payment.domain.service.refund.RefundPolicyFactory;
import com.sudo.railo.payment.domain.service.refund.RefundPolicyService;
import com.sudo.railo.payment.application.event.PaymentEventPublisher;
import com.sudo.railo.payment.exception.PaymentException;
import com.sudo.railo.payment.exception.RefundDeniedException;
import com.sudo.railo.booking.infra.JpaReservationRepository;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.train.application.TrainScheduleService;
import com.sudo.railo.train.domain.type.TrainOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 환불 처리 Application Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefundService {
    
    private final PaymentRepository paymentRepository;
    private final RefundCalculationRepository refundCalculationRepository;
    private final MileageTransactionRepository mileageTransactionRepository;
    private final MileageEarningScheduleRepository mileageEarningScheduleRepository;
    private final PaymentEventPublisher eventPublisher;
    private final JpaReservationRepository reservationRepository;
    private final TrainScheduleService trainScheduleService;
    private final RefundPolicyFactory refundPolicyFactory;
    private final RefundMetricService metricService;
    private final RefundAlertService alertService;
    private final RefundAuditLogService auditLogService;
    
    /**
     * 환불 계산 및 요청
     */
    public RefundCalculation calculateRefund(Long paymentId, RefundType refundType, 
                                           LocalDateTime trainDepartureTime, LocalDateTime trainArrivalTime, 
                                           String reason, String idempotencyKey) {
        log.info("환불 계산 시작 - paymentId: {}, refundType: {}, idempotencyKey: {}", 
                paymentId, refundType, idempotencyKey);
        
        // 메트릭: 환불 시도 기록
        metricService.recordAttempt();
        
        // 멱등성 체크: idempotencyKey가 있으면 기존 요청 확인
        if (idempotencyKey != null) {
            Optional<RefundCalculation> existing = refundCalculationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("멱등성 키로 기존 환불 계산 반환 - idempotencyKey: {}, refundCalculationId: {}", 
                        idempotencyKey, existing.get().getId());
                return existing.get();
            }
        }
        
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentException("결제 정보를 찾을 수 없습니다: " + paymentId));
        
        // 2. 환불 가능 여부 확인
        if (!payment.isRefundable()) {
            log.warn("환불 불가 - paymentId: {}, status: {}", paymentId, payment.getPaymentStatus());
            metricService.recordDeniedInvalidStatus();
            throw new PaymentException("환불 가능한 상태가 아닙니다: " + payment.getPaymentStatus());
        }
        
        // 3. 이미 환불 계산이 있는지 확인
        Optional<RefundCalculation> existingCalculation = refundCalculationRepository.findByPaymentId(paymentId);
        if (existingCalculation.isPresent()) {
            log.info("기존 환불 계산 반환 - paymentId: {}, refundCalculationId: {}", 
                    paymentId, existingCalculation.get().getId());
            return existingCalculation.get();
        }
        
        // 4. Payment에 저장된 열차 정보 사용 (예약이 삭제되어도 환불 가능)
        // 기존 결제는 trainScheduleId가 없을 수 있으므로 체크
        TrainScheduleService.TrainTimeInfo trainTimeInfo = null;
        if (payment.getTrainScheduleId() != null) {
            trainTimeInfo = trainScheduleService.getTrainTimeInfo(payment.getTrainScheduleId());
        } else {
            // 하위 호환성을 위해 예약 정보에서 가져오기 시도
            try {
                Reservation reservation = reservationRepository.findById(payment.getReservationId())
                    .orElse(null);
                if (reservation != null && reservation.getTrainSchedule() != null) {
                    trainTimeInfo = trainScheduleService.getTrainTimeInfo(
                        reservation.getTrainSchedule().getId());
                }
            } catch (Exception e) {
                log.warn("예약 정보 조회 실패, Payment에 저장된 시간 정보 사용 필요: {}", e.getMessage());
            }
        }
        
        // 실제 도착 시간 사용 (actualArrivalTime이 null이면 예정 도착 시간 사용)
        LocalDateTime actualArrivalTime = null;
        LocalDateTime departureTime = null;
        
        if (trainTimeInfo != null) {
            actualArrivalTime = trainTimeInfo.actualArrivalTime() != null 
                ? trainTimeInfo.actualArrivalTime() 
                : trainTimeInfo.scheduledArrivalTime();
            departureTime = trainTimeInfo.departureTime();
        } else if (payment.getTrainDepartureTime() != null && payment.getTrainArrivalTime() != null) {
            // Payment에 저장된 시간 정보 사용
            departureTime = payment.getTrainDepartureTime();
            actualArrivalTime = payment.getTrainArrivalTime();
            log.info("Payment에 저장된 열차 시간 정보 사용 - departureTime: {}, arrivalTime: {}", 
                    departureTime, actualArrivalTime);
        }
        
        // 필수 시간 정보 검증 - 파라미터로 전달된 경우도 고려
        if (departureTime == null || actualArrivalTime == null) {
            // 파라미터로 전달된 시간 정보 확인
            if (trainDepartureTime != null && trainArrivalTime != null) {
                departureTime = trainDepartureTime;
                actualArrivalTime = trainArrivalTime;
                log.info("파라미터로 전달된 열차 시간 정보 사용 - departureTime: {}, arrivalTime: {}", 
                        departureTime, actualArrivalTime);
            } else {
                throw new PaymentException("열차 시간 정보를 찾을 수 없습니다. paymentId: " + paymentId);
            }
        }
        
        // 파라미터로 받은 시간이 없으면 자동으로 조회한 시간 사용
        if (trainDepartureTime == null) {
            trainDepartureTime = departureTime;
            log.info("열차 출발 시간 자동 조회: {}", trainDepartureTime);
        }
        if (trainArrivalTime == null) {
            trainArrivalTime = actualArrivalTime;
            log.info("열차 도착 시간 자동 조회: {}", trainArrivalTime);
        }
        
        // 5. 지연 정보 조회
        int delayMinutes = 0;
        if (trainTimeInfo != null && trainTimeInfo.delayMinutes() != null) {
            delayMinutes = trainTimeInfo.delayMinutes();
            log.info("열차 지연 정보 - 지연시간: {}분", delayMinutes);
        }
        
        // 6. 도착 시간 이후 환불 불가 체크 (지연 시간 고려)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refundDeadline = trainArrivalTime.plusMinutes(delayMinutes); // 지연 시간만큼 환불 가능 시간 연장
        
        if (now.isAfter(refundDeadline)) {
            log.warn("환불 불가 - 열차 도착 후 - paymentId: {}, 현재: {}, 도착: {}, 지연: {}분", 
                    paymentId, now, trainArrivalTime, delayMinutes);
            metricService.recordDeniedAfterArrival();
            alertService.alertRefundDenied(paymentId, "열차 도착 후 환불 시도");
            // 중요한 이벤트이므로 감사 로그 저장
            auditLogService.logRefundDeniedAfterArrival(
                paymentId, 
                payment.getReservationId(), 
                payment.getMemberId(),
                String.format("현재시간: %s, 도착시간: %s, 지연: %d분", now, trainArrivalTime, delayMinutes)
            );
            
            String message = delayMinutes > 0 
                ? String.format("열차가 도착한 이후에는 환불이 불가능합니다. 도착 시간: %s (지연 %d분 포함)", 
                    refundDeadline.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), delayMinutes)
                : String.format("열차가 도착한 이후에는 환불이 불가능합니다. 도착 시간: %s", 
                    trainArrivalTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            
            throw new RefundDeniedException(message);
        }
        
        // 6. 환불 수수료율 계산 (실제 도착 시간 기준)
        BigDecimal refundFeeRate;
        
        // 강제 환불 (운행 취소, 변경 등)인 경우 수수료 없음
        if (refundType == RefundType.CHANGE) {
            refundFeeRate = BigDecimal.ZERO;
        } else {
            // 열차 운영사 정보 가져오기
            TrainOperator operator = getTrainOperator(payment);
            
            // 운영사에 맞는 환불 정책 조회
            RefundPolicyService refundPolicy = refundPolicyFactory.getPolicy(operator);
            
            // 환불 수수료율 계산
            refundFeeRate = refundPolicy.calculateRefundFeeRate(trainDepartureTime, trainArrivalTime, now);
            
            log.info("환불 정책 적용 - 운영사: {}, 정책: {}, 수수료율: {}%", 
                    operator, refundPolicy.getPolicyName(), refundFeeRate.multiply(BigDecimal.valueOf(100)));
        }
        
        // 6. 환불 금액 계산
        BigDecimal originalAmount = payment.getAmountPaid();
        BigDecimal mileageUsed = payment.getMileagePointsUsed();
        BigDecimal refundFee = originalAmount.multiply(refundFeeRate);
        BigDecimal refundAmount = originalAmount.subtract(refundFee);
        
        // 8. 환불 계산 생성 (실제 도착 시간 사용)
        RefundCalculation refundCalculation = RefundCalculation.builder()
            .paymentId(paymentId)
            .reservationId(payment.getReservationId())
            .memberId(payment.getMemberId())
            .idempotencyKey(idempotencyKey != null ? idempotencyKey : generateIdempotencyKey(paymentId))
            .originalAmount(originalAmount)
            .mileageUsed(mileageUsed)
            .refundFeeRate(refundFeeRate)
            .refundFee(refundFee)
            .refundAmount(refundAmount)
            .mileageRefundAmount(mileageUsed) // 사용한 마일리지는 전액 복원
            .trainDepartureTime(trainDepartureTime)  // 파라미터 또는 자동 조회된 시간
            .trainArrivalTime(trainArrivalTime)  // 파라미터 또는 자동 조회된 시간
            .refundRequestTime(now)
            .refundType(refundType)
            .refundStatus(RefundStatus.PENDING)
            .refundReason(reason != null ? reason : "사용자 요청")  // 환불 사유가 없으면 기본값 설정
            .build();
        
        RefundCalculation savedRefundCalculation = refundCalculationRepository.save(refundCalculation);
        
        log.info("환불 계산 완료 - refundCalculationId: {}, refundAmount: {}, refundFee: {}", 
                savedRefundCalculation.getId(), refundAmount, refundFee);
        
        // 메트릭: 환불 계산 성공
        metricService.recordSuccess();
        
        return savedRefundCalculation;
    }
    
    /**
     * 환불 처리 실행
     */
    public void processRefund(Long refundCalculationId) {
        log.info("환불 처리 시작 - refundCalculationId: {}", refundCalculationId);
        
        try {
            // 1. 환불 계산 조회
            RefundCalculation refundCalculation = refundCalculationRepository.findById(refundCalculationId)
                .orElseThrow(() -> new PaymentException("환불 계산을 찾을 수 없습니다: " + refundCalculationId));
            
            if (refundCalculation.getRefundStatus() != RefundStatus.PENDING) {
                throw new PaymentException("처리 대기 상태가 아닙니다: " + refundCalculation.getRefundStatus());
            }
            
            // 2. 결제 정보 조회
            Payment payment = paymentRepository.findById(refundCalculation.getPaymentId())
                .orElseThrow(() -> new PaymentException("결제 정보를 찾을 수 없습니다: " + refundCalculation.getPaymentId()));
            
            // 3. 환불 처리 상태로 변경
            refundCalculation.updateRefundStatus(RefundStatus.PROCESSING);
            refundCalculationRepository.save(refundCalculation);
            
            // 4. 마일리지 환불 처리 (사용한 마일리지 복원)
            if (refundCalculation.getMileageUsed().compareTo(BigDecimal.ZERO) > 0) {
                processMileageRefund(refundCalculation);
            }
            
            // 5. PG사 환불 요청 (실제 구현 시 PG사 API 호출)
            String pgRefundTransactionId;
            try {
                pgRefundTransactionId = processPgRefund(refundCalculation);
            } catch (Exception e) {
                // 네트워크 오류 등으로 결과를 알 수 없는 경우
                log.error("PG사 환불 요청 실패 - Unknown 상태로 저장", e);
                refundCalculation.updateRefundStatus(RefundStatus.UNKNOWN);
                refundCalculationRepository.save(refundCalculation);
                alertService.alertRefundFailure(payment.getId(), "PG 통신 오류", e.getMessage());
                // Unknown 상태도 중요하므로 기록
                auditLogService.logUnknownState(payment.getId(), e.getMessage());
                throw new PaymentException("PG사 통신 오류로 환불 상태를 확인할 수 없습니다. 잠시 후 재시도됩니다.");
            }
            
            // 6. Payment 엔티티 환불 처리
            PaymentExecutionStatus previousStatus = payment.getPaymentStatus();
            
            Payment.RefundRequest refundRequest = Payment.RefundRequest.builder()
                .refundAmount(refundCalculation.getRefundAmount())
                .refundFee(refundCalculation.getRefundFee())
                .reason(refundCalculation.getRefundReason())
                .pgTransactionId(pgRefundTransactionId)
                .pgApprovalNo("REFUND_" + System.currentTimeMillis())
                .build();
            payment.processRefund(refundRequest);
            paymentRepository.save(payment);
            
            // 7. 환불 상태 변경 이벤트 발행
            // PaymentEventTranslator가 PaymentStateChangedEvent를 수신하여 
            // BookingPaymentRefundedEvent로 변환하므로 추가 이벤트 발행 불필요
            eventPublisher.publishPaymentStateChanged(
                payment,
                previousStatus,
                PaymentExecutionStatus.REFUNDED,
                refundCalculation.getRefundReason(),
                "ADMIN"
            );
            
            // 8. 마일리지 적립 스케줄 취소
            cancelMileageEarningSchedule(payment.getId().toString());
            
            // 9. 환불 완료 처리
            refundCalculation.markAsProcessed();
            refundCalculationRepository.save(refundCalculation);
            
            log.info("환불 처리 완료 - refundCalculationId: {}, pgRefundTransactionId: {}", 
                    refundCalculationId, pgRefundTransactionId);
            
        } catch (Exception e) {
            log.error("환불 처리 실패 - refundCalculationId: {}", refundCalculationId, e);
            
            // 환불 계산이 이미 조회된 경우에만 실패 상태로 변경
            try {
                RefundCalculation refundCalculation = refundCalculationRepository.findById(refundCalculationId)
                    .orElse(null);
                if (refundCalculation != null) {
                    refundCalculation.markAsFailed(e.getMessage());
                    refundCalculationRepository.save(refundCalculation);
                }
            } catch (Exception saveException) {
                log.error("환불 실패 상태 저장 중 오류 발생", saveException);
            }
            
            throw new PaymentException("환불 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 마일리지 환불 처리
     */
    private void processMileageRefund(RefundCalculation refundCalculation) {
        if (refundCalculation.getMemberId() == null) {
            return; // 비회원은 마일리지 환불 불가
        }
        
        // 사용한 마일리지 복원
        MileageTransaction refundTransaction = MileageTransaction.builder()
            .memberId(refundCalculation.getMemberId())
            .type(MileageTransaction.TransactionType.REFUND)
            .pointsAmount(refundCalculation.getMileageUsed())
            .description("환불로 인한 마일리지 복원 - 예약번호: " + refundCalculation.getReservationId())
            .paymentId(refundCalculation.getPaymentId().toString())
            .expiresAt(LocalDateTime.now().plusYears(5)) // 5년 유효
            .status(MileageTransaction.TransactionStatus.COMPLETED)
            .build();
        
        mileageTransactionRepository.save(refundTransaction);
        
        log.info("마일리지 환불 완료 - memberId: {}, amount: {}", 
                refundCalculation.getMemberId(), refundCalculation.getMileageUsed());
    }
    
    /**
     * PG사 환불 요청 (Mock 구현)
     */
    private String processPgRefund(RefundCalculation refundCalculation) {
        // 실제 구현 시 PG사 API 호출
        // 현재는 Mock 응답 반환
        String mockRefundTransactionId = "REFUND_" + System.currentTimeMillis();
        
        log.info("PG 환불 요청 완료 - refundAmount: {}, transactionId: {}", 
                refundCalculation.getRefundAmount(), mockRefundTransactionId);
        
        return mockRefundTransactionId;
    }
    
    /**
     * 환불 계산 조회
     */
    @Transactional(readOnly = true)
    public Optional<RefundCalculation> getRefundCalculation(Long refundCalculationId) {
        return refundCalculationRepository.findById(refundCalculationId);
    }
    
    /**
     * 결제별 환불 계산 조회
     */
    @Transactional(readOnly = true)
    public Optional<RefundCalculation> getRefundCalculationByPaymentId(Long paymentId) {
        return refundCalculationRepository.findByPaymentId(paymentId);
    }
    
    /**
     * 회원별 환불 내역 조회
     */
    @Transactional(readOnly = true)
    public List<RefundCalculation> getRefundHistoryByMember(Long memberId) {
        return refundCalculationRepository.findByMemberId(memberId);
    }
    
    /**
     * 처리 대기 중인 환불 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RefundCalculation> getPendingRefunds() {
        return refundCalculationRepository.findPendingRefunds();
    }
    
    /**
     * 환불 취소
     */
    public void cancelRefund(Long refundCalculationId, String reason) {
        RefundCalculation refundCalculation = refundCalculationRepository.findById(refundCalculationId)
            .orElseThrow(() -> new PaymentException("환불 계산을 찾을 수 없습니다: " + refundCalculationId));
        
        if (refundCalculation.getRefundStatus() != RefundStatus.PENDING) {
            throw new PaymentException("대기 상태의 환불만 취소할 수 있습니다: " + refundCalculation.getRefundStatus());
        }
        
        refundCalculation.updateRefundStatus(RefundStatus.CANCELLED);
        refundCalculation.updateRefundReason(reason);
        refundCalculationRepository.save(refundCalculation);
        
        log.info("환불 취소 완료 - refundCalculationId: {}, reason: {}", refundCalculationId, reason);
    }
    
    /**
     * 마일리지 적립 스케줄 취소
     * 환불 처리 시 예정된 마일리지 적립을 취소
     */
    private void cancelMileageEarningSchedule(String paymentId) {
        try {
            Optional<MileageEarningSchedule> schedule = 
                mileageEarningScheduleRepository.findByPaymentId(paymentId);
            
            if (schedule.isPresent()) {
                MileageEarningSchedule earningSchedule = schedule.get();
                
                // SCHEDULED 또는 READY 상태의 스케줄만 취소 가능
                if (earningSchedule.getStatus() == MileageEarningSchedule.EarningStatus.SCHEDULED || 
                    earningSchedule.getStatus() == MileageEarningSchedule.EarningStatus.READY) {
                    
                    // 실패 상태로 변경하여 처리되지 않도록 함
                    earningSchedule.fail("환불로 인한 마일리지 적립 취소");
                    mileageEarningScheduleRepository.save(earningSchedule);
                    
                    log.info("마일리지 적립 스케줄 취소 완료 - 스케줄ID: {}, 결제ID: {}", 
                            earningSchedule.getId(), paymentId);
                } else {
                    log.warn("마일리지 적립 스케줄이 이미 처리됨 - 스케줄ID: {}, 상태: {}", 
                            earningSchedule.getId(), earningSchedule.getStatus());
                }
            } else {
                log.debug("취소할 마일리지 적립 스케줄이 없음 - 결제ID: {}", paymentId);
            }
        } catch (Exception e) {
            log.error("마일리지 적립 스케줄 취소 중 오류 발생 - 결제ID: {}", paymentId, e);
            // 마일리지 스케줄 취소 실패가 환불 전체를 실패시키지 않도록 예외를 전파하지 않음
        }
    }
    
    /**
     * 운영사 정보 가져오기
     * 1. Payment에 저장된 운영사 정보 사용
     * 2. 없으면 예약에서 추출
     * 3. 그것도 없으면 기본값 KORAIL
     */
    private TrainOperator getTrainOperator(Payment payment) {
        // 1. Payment에 저장된 운영사 정보 우선 사용
        if (payment.getTrainOperator() != null) {
            return payment.getTrainOperator();
        }
        
        // 2. 하위 호환성을 위해 예약에서 추출 시도
        try {
            Reservation reservation = reservationRepository.findById(payment.getReservationId())
                .orElse(null);
            if (reservation != null && reservation.getTrainSchedule() != null && 
                reservation.getTrainSchedule().getTrain() != null) {
                String trainName = reservation.getTrainSchedule().getTrain().getTrainName();
                
                // KTX나 산천은 KORAIL 운영
                if (trainName != null && (trainName.contains("KTX") || trainName.contains("산천") || 
                                         trainName.contains("ITX") || trainName.contains("새마을"))) {
                    return TrainOperator.KORAIL;
                }
                
                // SRT 관련 이름이면 SRT 운영
                if (trainName != null && trainName.contains("SRT")) {
                    return TrainOperator.SRT;
                }
            }
        } catch (Exception e) {
            log.error("예약에서 운영사 정보 추출 중 오류 발생", e);
        }
        
        // 3. 기본값은 KORAIL
        log.warn("운영사 정보를 확인할 수 없습니다. 기본값(KORAIL)을 사용합니다.");
        return TrainOperator.KORAIL;
    }
    
    /**
     * 멱등성 키 자동 생성
     * 동일한 paymentId와 시간으로 생성하여 중복 방지
     */
    private String generateIdempotencyKey(Long paymentId) {
        return String.format("refund_%d_%d_%s", 
                paymentId, 
                System.currentTimeMillis() / 1000,  // 초 단위
                UUID.randomUUID().toString().substring(0, 8));
    }
} 