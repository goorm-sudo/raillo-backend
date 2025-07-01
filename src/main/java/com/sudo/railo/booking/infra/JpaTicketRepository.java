package com.sudo.railo.booking.infra;

import com.sudo.railo.booking.domain.PaymentStatus;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.domain.TicketRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TicketRepository의 JPA 구현체
 * Spring Data JPA를 활용한 Infrastructure 계층 구현
 * JpaRepository가 기본 CRUD 메서드들을 자동 제공
 */
@Repository
public interface JpaTicketRepository extends JpaRepository<Ticket, Long>, TicketRepository {
    
    /**
     * 예약 ID로 관련된 모든 티켓 조회
     */
    @Query("SELECT t FROM Ticket t WHERE t.reservation.id = :reservationId")
    List<Ticket> findByReservationId(@Param("reservationId") Long reservationId);
    
    /**
     * 결제 상태로 티켓 목록 조회 (Spring Data JPA 자동 쿼리)
     */
    List<Ticket> findByPaymentStatus(PaymentStatus paymentStatus);
} 