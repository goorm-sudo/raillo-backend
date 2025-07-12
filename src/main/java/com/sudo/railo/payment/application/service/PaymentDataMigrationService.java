package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.infra.JpaReservationRepository;
import com.sudo.railo.train.domain.ScheduleStop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기존 결제 데이터에 열차 정보를 추가하는 마이그레이션 서비스
 * 
 * 수동으로 실행하거나 ApplicationRunner로 실행 가능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentDataMigrationService {
    
    private final PaymentRepository paymentRepository;
    private final JpaReservationRepository reservationRepository;
    
    /**
     * 열차 정보가 없는 결제 데이터를 마이그레이션
     */
    public void migratePaymentTrainInfo() {
        log.info("결제 데이터 마이그레이션 시작");
        
        // 1. 열차 정보가 없는 결제 조회
        List<Payment> paymentsWithoutTrainInfo = paymentRepository.findAll().stream()
                .filter(payment -> payment.getTrainScheduleId() == null || payment.getTrainArrivalTime() == null)
                .filter(payment -> payment.getReservationId() != null)
                .toList();
        
        log.info("마이그레이션 대상 결제 수: {}", paymentsWithoutTrainInfo.size());
        
        int migratedCount = 0;
        int failedCount = 0;
        
        for (Payment payment : paymentsWithoutTrainInfo) {
            try {
                // 2. 예약 정보 조회 (삭제된 것도 포함)
                Reservation reservation = reservationRepository.findById(payment.getReservationId())
                        .orElse(null);
                
                if (reservation == null || reservation.getTrainSchedule() == null) {
                    log.warn("예약 정보를 찾을 수 없음 - paymentId: {}, reservationId: {}", 
                            payment.getId(), payment.getReservationId());
                    failedCount++;
                    continue;
                }
                
                // 3. 열차 정보 업데이트 (리플렉션 사용)
                boolean updated = false;
                
                if (payment.getTrainScheduleId() == null) {
                    updateTrainScheduleId(payment, reservation.getTrainSchedule().getId());
                    updated = true;
                }
                
                // 4. 도착 시간 계산 및 설정
                if (payment.getTrainArrivalTime() == null) {
                    LocalDateTime arrivalTime = calculateArrivalTime(reservation);
                    if (arrivalTime != null) {
                        updateTrainArrivalTime(payment, arrivalTime);
                        updated = true;
                    }
                }
                
                if (!updated) {
                    continue;
                }
                
                // 5. 저장
                paymentRepository.save(payment);
                migratedCount++;
                
                log.debug("결제 데이터 마이그레이션 완료 - paymentId: {}, trainScheduleId: {}, arrivalTime: {}", 
                        payment.getId(), payment.getTrainScheduleId(), payment.getTrainArrivalTime());
                
            } catch (Exception e) {
                log.error("결제 데이터 마이그레이션 실패 - paymentId: {}", payment.getId(), e);
                failedCount++;
            }
        }
        
        log.info("결제 데이터 마이그레이션 완료 - 성공: {}, 실패: {}", migratedCount, failedCount);
    }
    
    /**
     * 예약 정보로부터 도착 시간 계산
     */
    private LocalDateTime calculateArrivalTime(Reservation reservation) {
        if (reservation.getTrainSchedule() == null || reservation.getArrivalStation() == null) {
            return null;
        }
        
        // Schedule stops에서 도착역 찾기
        for (ScheduleStop stop : reservation.getTrainSchedule().getScheduleStops()) {
            if (stop.getStation().getId().equals(reservation.getArrivalStation().getId())) {
                return reservation.getTrainSchedule().getOperationDate()
                        .atTime(stop.getArrivalTime());
            }
        }
        
        return null;
    }
    
    /**
     * 마이그레이션 상태 확인
     */
    public void checkMigrationStatus() {
        List<Payment> allPayments = paymentRepository.findAll();
        long totalPayments = allPayments.size();
        long paymentsWithTrainInfo = allPayments.stream()
                .filter(payment -> payment.getTrainScheduleId() != null && payment.getTrainArrivalTime() != null)
                .count();
        long paymentsMissingInfo = totalPayments - paymentsWithTrainInfo;
        
        log.info("마이그레이션 상태 - 전체: {}, 완료: {}, 미완료: {}", 
                totalPayments, paymentsWithTrainInfo, paymentsMissingInfo);
    }
    
    /**
     * 리플렉션을 사용하여 trainScheduleId 업데이트
     */
    private void updateTrainScheduleId(Payment payment, Long trainScheduleId) {
        try {
            Field field = Payment.class.getDeclaredField("trainScheduleId");
            field.setAccessible(true);
            field.set(payment, trainScheduleId);
        } catch (Exception e) {
            throw new RuntimeException("trainScheduleId 설정 실패", e);
        }
    }
    
    /**
     * 리플렉션을 사용하여 trainArrivalTime 업데이트
     */
    private void updateTrainArrivalTime(Payment payment, LocalDateTime arrivalTime) {
        try {
            Field field = Payment.class.getDeclaredField("trainArrivalTime");
            field.setAccessible(true);
            field.set(payment, arrivalTime);
        } catch (Exception e) {
            throw new RuntimeException("trainArrivalTime 설정 실패", e);
        }
    }
}