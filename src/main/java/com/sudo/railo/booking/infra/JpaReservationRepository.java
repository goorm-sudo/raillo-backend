package com.sudo.railo.booking.infra;

import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.ReservationRepository;
import com.sudo.railo.booking.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ReservationRepository의 JPA 구현체
 * Spring Data JPA를 활용한 Infrastructure 계층 구현
 */
@Repository
public interface JpaReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepository {
    
    /**
     * 예약 번호로 조회
     */
    @Query("SELECT r FROM Reservation r WHERE r.reservationNumber = :reservationNumber")
    Optional<Reservation> findByReservationNumber(@Param("reservationNumber") Long reservationNumber);
    
    /**
     * 회원 ID로 예약 목록 조회 (최신순 정렬)
     */
    @Query("SELECT r FROM Reservation r WHERE r.member.id = :memberId ORDER BY r.id DESC")
    List<Reservation> findByMemberId(@Param("memberId") Long memberId);
    
    /**
     * 예약 상태로 조회
     */
    List<Reservation> findByReservationStatus(ReservationStatus status);
    

} 