package com.sudo.railo.booking.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.SeatInventory;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.SeatInventoryRepository;
import com.sudo.railo.global.exception.error.BusinessException;

import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatInventoryService {

    private final SeatInventoryRepository seatInventoryRepository;

    /***
     * 좌석을 예약하는 메서드
     * @param trainScheduleId - 열차 스케줄 ID
     * @param seatId - 좌석 ID
     */
    @Transactional
    public void reserveSeat(Long trainScheduleId, Long seatId) {
        try {
            Optional<SeatInventory> result = seatInventoryRepository.findByTrainScheduleIdAndSeatId(trainScheduleId, seatId);
            if (!result.isPresent()) {
                throw new BusinessException(BookingError.SEAT_NOT_FOUND);
            }
            SeatInventory seatInventory = result.get();
            seatInventory.reserveSeat();
            seatInventoryRepository.save(seatInventory);
        } catch (OptimisticLockException e) {
            // 동시성 문제 발생
            throw new BusinessException(BookingError.SEAT_ALREADY_RESERVED);
        } catch (Exception e) {
            throw new BusinessException(BookingError.SEAT_RESERVATION_FAILED);
        }
    }

    /***
     * 좌석 예약을 취소하는 메서드
     * @param trainScheduleId - 열차 스케줄 ID
     * @param seatId - 좌석 ID
     */
    @Transactional
    public void cancelReservation(Long trainScheduleId, Long seatId) {
        try {
            Optional<SeatInventory> result = seatInventoryRepository.findByTrainScheduleIdAndSeatId(trainScheduleId, seatId);
            if (!result.isPresent()) {
                throw new BusinessException(BookingError.SEAT_NOT_FOUND);
            }
            SeatInventory seatInventory = result.get();
            seatInventory.cancelReservation();
            seatInventoryRepository.save(seatInventory);
        } catch (OptimisticLockException e) {
            // 동시성 문제 발생
            throw new BusinessException(BookingError.SEAT_ALREADY_CANCELLED);
        } catch (Exception e) {
            throw new BusinessException(BookingError.SEAT_CANCELLATION_FAILED);
        }
    }

    /***
     * 예약 만료 시간을 기준으로 만료된 좌석을 취소하는 메서드
     */
    @Transactional
    public void cancelExpiredReservation() {
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(10);
        List<SeatInventory> expiredSeats = seatInventoryRepository.findExpiredSeats(expiredAt);
        for (SeatInventory seatInventory : expiredSeats) {
            seatInventory.cancelReservation();
            seatInventoryRepository.save(seatInventory);
        }
    }
}
