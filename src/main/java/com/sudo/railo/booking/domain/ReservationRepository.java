package com.sudo.railo.booking.domain;

import java.util.List;
import java.util.Optional;

/**
 * 예약(Reservation) Repository 인터페이스
 * Domain 계층에서 정의하고 Infrastructure 계층에서 구현
 * 
 * JpaRepository의 기본 CRUD 메서드는 Infrastructure 계층에서 상속받으므로
 * 여기서는 도메인 특화 메서드만 정의
 */
public interface ReservationRepository {
    
    // ===== 도메인 특화 메서드들 =====
    /**
     * 예약 코드로 조회
     */
    Optional<Reservation> findByReservationNumber(String reservationCode);
    
    /**
     * 회원 ID로 예약 목록 조회
     */
    List<Reservation> findByMemberId(Long memberId);
    
    /**
     * 예약 상태로 조회
     */
    List<Reservation> findByReservationStatus(ReservationStatus status);
} 