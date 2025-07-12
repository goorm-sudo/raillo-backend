package com.sudo.railo.booking.domain;

import java.util.List;
import java.util.Optional;

/**
 * 티켓(Ticket) Repository 인터페이스
 * Domain 계층에서 정의하고 Infrastructure 계층에서 구현
 * 
 * JpaRepository의 기본 CRUD 메서드는 상속받으므로 도메인 특화 메서드만 정의
 */
public interface TicketRepository {
    
    // ===== 도메인 특화 메서드들 =====
    /**
     * 예약 ID로 관련된 모든 티켓 조회
     */
    List<Ticket> findByReservationId(Long reservationId);
    
    /**
     * 결제 상태로 티켓 목록 조회
     */
    List<Ticket> findByPaymentStatus(PaymentStatus paymentStatus);
} 