package com.sudo.railo.payment.infrastructure.adapter.out.persistence;

import com.sudo.railo.payment.application.port.out.LoadPaymentPort;
import com.sudo.railo.payment.application.port.out.SavePaymentPort;
import com.sudo.railo.payment.domain.entity.Payment;
import com.sudo.railo.payment.domain.repository.PaymentRepository;
import com.sudo.railo.payment.infrastructure.persistence.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 영속성 어댑터
 * 
 * 애플리케이션 계층의 포트를 구현하여 실제 데이터베이스 접근을 담당
 * 헥사고날 아키텍처의 아웃바운드 어댑터
 */
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements LoadPaymentPort, SavePaymentPort {
    
    private final JpaPaymentRepository paymentRepository;
    
    @Override
    public Optional<Payment> findById(Long paymentId) {
        return ((PaymentRepository) paymentRepository).findById(paymentId);
    }
    
    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.existsByIdempotencyKey(idempotencyKey);
    }
    
    @Override
    public Optional<Payment> findByExternalOrderId(String externalOrderId) {
        return paymentRepository.findByExternalOrderId(externalOrderId);
    }
    
    @Override
    public Optional<Payment> findByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }
    
    @Override
    public Payment save(Payment payment) {
        return ((PaymentRepository) paymentRepository).save(payment);
    }
    
    @Override
    public Page<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable) {
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }
    
    @Override
    public Page<Payment> findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long memberId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return paymentRepository.findByMemberIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                memberId, startDate, endDate, pageable);
    }
    
    @Override
    public Page<Payment> findByNonMemberInfo(String name, String phoneNumber, Pageable pageable) {
        return paymentRepository.findByNonMemberNameAndNonMemberPhoneOrderByCreatedAtDesc(
                name, phoneNumber, pageable);
    }
}