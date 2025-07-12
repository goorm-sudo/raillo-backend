package com.sudo.railo.booking.application;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.booking.application.dto.ReservationInfo;
import com.sudo.railo.booking.application.dto.projection.SeatReservationProjection;
import com.sudo.railo.booking.application.dto.request.FareCalculateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.application.dto.response.ReservationDetail;
import com.sudo.railo.booking.application.dto.response.SeatReservationDetail;
import com.sudo.railo.booking.config.BookingConfig;
import com.sudo.railo.booking.domain.PassengerSummary;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.ReservationStatus;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.ReservationRepository;
import com.sudo.railo.booking.infra.ReservationRepositoryCustom;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;
import com.sudo.railo.train.domain.Station;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.StationRepository;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ObjectMapper objectMapper;
	private final BookingConfig bookingConfig;
	private final FareCalculationService fareCalculationService;
	private final TrainScheduleRepository trainScheduleRepository;
	private final MemberRepository memberRepository;
	private final StationRepository stationRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationRepositoryCustom reservationRepositoryCustom;

	/***
	 * 고객용 예매번호를 생성하는 메서드
	 * @return 고객용 예매번호
	 */
	private String generateReservationCode() {
		// yyyyMMddHHmmss<랜덤4자리> 형식
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String dateTimeStr = now.format(formatter);

		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder randomStr = new StringBuilder();
		SecureRandom secureRandom = new SecureRandom();
		for (int i = 0; i < 4; i++) {
			int idx = secureRandom.nextInt(chars.length());
			randomStr.append(chars.charAt(idx));
		}
		return dateTimeStr + randomStr;
	}

	/***
	 * 예약을 생성하는 메서드
	 * @param request 예약 생성 요청 DTO
	 * @return 예약 레코드
	 */
	@Transactional
	public Reservation createReservation(ReservationCreateRequest request, UserDetails userDetails) {
		try {
			TrainSchedule trainSchedule = trainScheduleRepository.findById(request.trainScheduleId())
				.orElseThrow(() -> new BusinessException((TrainErrorCode.TRAIN_SCHEDULE_NOT_FOUND)));

			if (trainSchedule.getOperationStatus() == OperationStatus.CANCELLED) {
				throw new BusinessException(TrainErrorCode.TRAIN_OPERATION_CANCELLED);
			}

			Member member = memberRepository.findByMemberNo(userDetails.getUsername())
				.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

			Station departureStation = stationRepository.findById(request.departureStationId())
				.orElseThrow(() -> new BusinessException(TrainErrorCode.STATION_NOT_FOUND));

			Station arrivalStation = stationRepository.findById(request.arrivalStationId())
				.orElseThrow(() -> new BusinessException(TrainErrorCode.STATION_NOT_FOUND));

			List<PassengerSummary> passengerSummary = request.passengers();
			LocalDateTime now = LocalDateTime.now();
			Reservation reservation = Reservation.builder()
				.trainSchedule(trainSchedule)
				.member(member)
				.reservationCode(generateReservationCode())
				.tripType(request.tripType())
				.totalPassengers(passengerSummary.stream().mapToInt(PassengerSummary::getCount).sum())
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
			reservationRepository.deleteById(request.reservationId());
		} catch (Exception e) {
			throw new BusinessException(BookingError.RESERVATION_DELETE_FAILED);
		}
	}

	/***
	 * 만료된 예약을 일괄삭제하는 메서드
	 */
	@Transactional
	public void expireReservations() {
		LocalDateTime now = LocalDateTime.now();
		reservationRepository.deleteAllByExpiresAtBefore(now);
	}

	/**
	 * 예약을 조회하는 메서드
	 * @param memberNo 회원 번호
	 * @param reservationId 예약 ID
	 * @return 예약
	 */
	@Transactional(readOnly = true)
	public ReservationDetail getReservation(String memberNo, Long reservationId) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		List<ReservationInfo> reservationInfos = reservationRepositoryCustom.findReservationDetail(
			member.getId(), List.of(reservationId));

		if (reservationInfos.isEmpty()) {
			throw new BusinessException(BookingError.RESERVATION_NOT_FOUND);
		}

		return convertToReservationDetail(reservationInfos).get(0);
	}

	/**
	 * 예약 목록을 조회하는 메서드
	 * @param memberNo 회원 번호
	 * @return 예약 목록
	 */
	@Transactional(readOnly = true)
	public List<ReservationDetail> getReservations(String memberNo) {
		Member member = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));

		// 예약 조회
		List<ReservationInfo> reservationInfos = reservationRepositoryCustom.findReservationDetail(member.getId());
		return convertToReservationDetail(reservationInfos);
	}

	public List<ReservationDetail> convertToReservationDetail(List<ReservationInfo> reservationInfos) {
		return reservationInfos.stream()
			.map(info -> ReservationDetail.of(
				info.reservationId(),
				info.reservationCode(),
				String.format("%03d", info.trainNumber()),
				info.trainName(),
				info.departureStationName(),
				info.arrivalStationName(),
				info.departureTime(),
				info.arrivalTime(),
				info.operationDate(),
				info.expiresAt(),
				convertToSeatReservationDetail(info.seats())
			))
			.toList();
	}

	private List<SeatReservationDetail> convertToSeatReservationDetail(List<SeatReservationProjection> projection) {
		return projection.stream()
			.map(p -> SeatReservationDetail.of(
				p.getSeatReservationId(),
				p.getPassengerType(),
				p.getCarNumber(),
				p.getCarType(),
				p.getSeatNumber(),
				p.getFare(),
				// 운임 계산
				fareCalculationService.calculateFare(new FareCalculateRequest(
					p.getPassengerType(),
					BigDecimal.valueOf(p.getFare()))
				).intValue()
			))
			.toList();
	}
}
