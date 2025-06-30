package com.sudo.railo.booking.application;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.config.BookingConfig;
import com.sudo.railo.booking.domain.PassengerSummary;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.ReservationStatus;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.ReservationRepository;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ObjectMapper objectMapper;
	private final BookingConfig bookingConfig;
	private final TrainScheduleRepository trainScheduleRepository;
	private final MemberRepository memberRepository;
	private final StationRepository stationRepository;
	private final ReservationRepository reservationRepository;

	/***
	 * 고객용 예매번호를 생성하는 메서드
	 * @return 고객용 예매번호
	 */
	private String generateReservationNumber() {
		// TODO: 고객용 예매번호 생성 로직 구현, 스펙 확정 시 수행
		return "";
	}

	/***
	 * 예약을 생성하는 메서드
	 * @param request 예약 생성 요청 DTO
	 * @return 예약 레코드
	 */
	@Transactional
	public Reservation createReservation(ReservationCreateRequest request, UserDetails userDetails) {
		try {
			// TODO: 조회된 운행 스케줄이 없을때 적절한 오류를 반환해야 합니다.
			TrainSchedule trainSchedule = trainScheduleRepository.findById(request.getTrainScheduleId())
				.orElseThrow(Exception::new);

			if (trainSchedule.getOperationStatus() != OperationStatus.ACTIVE) {
				// TODO: 스케줄 운행 여부에 따라 적절한 오류를 반환해야 합니다.
				// throw new BusinessException(BookingError.TRAIN_NOT_OPERATIONAL);
			}

			// 멤버
			Member member = memberRepository.findByMemberNo(userDetails.getUsername())
				.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

			// TODO: 조회된 출발역이 없을 때 적절한 오류를 반환해야 합니다.
			Station departureStation = stationRepository.findById(request.getDepartureStationId())
				.orElseThrow(Exception::new);

			// TODO: 조회된 도착역이 없을 때 적절한 오류를 반환해야 합니다.
			Station arrivalStation = stationRepository.findById(request.getArrivalStationId())
				.orElseThrow(Exception::new);

			// 승객 유형별 정보
			PassengerSummary passengerSummary = request.getPassengerSummary();

			// 예약 완료 시간, 만료 시간
			LocalDateTime now = LocalDateTime.now();

			// Reservation Entity
			Reservation reservation = Reservation.builder()
				.trainSchedule(trainSchedule)
				.member(member)
				//.reservationNumber()
				.tripType(request.getTripType())
				.totalPassengers(passengerSummary.getPassengerCount())
				.passengerSummary(objectMapper.writeValueAsString(passengerSummary))
				.reservationStatus(ReservationStatus.RESERVED)
				.expiresAt(now.plusMinutes(bookingConfig.getExpiration().getReservation()))
				.reservedAt(now)
				.departureStation(departureStation)
				.arrivalStation(arrivalStation)
				.build();
			return reservationRepository.save(reservation);
		} catch (Exception e) {
			throw new BusinessException(BookingError.RESERVATION_CREATE_FAILED);
		}
	}

	/***
	 * 예약 번호로 예약을 삭제하는 메서드
	 * @param request 예약 삭제 요청 DTO
	 */
	@Transactional
	public void deleteReservation(ReservationDeleteRequest request) {
		try {
			reservationRepository.deleteById(request.getReservationId());
		} catch (Exception e) {
			throw new BusinessException(BookingError.RESERVATION_DELETE_FAILED);
		}
	}

	/***
	 * 만료된 예약을 일괄삭제하는 메서드
	 */
	@Transactional
	public void expireReservations() {
		int reservationExpirationTime = bookingConfig.getExpiration().getReservation();
		LocalDateTime now = LocalDateTime.now();
		reservationRepository.deleteAllByExpiresAtBefore(now);
	}
}
