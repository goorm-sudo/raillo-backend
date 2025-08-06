package com.sudo.railo.booking.application;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudo.railo.booking.application.dto.ReservationInfo;
import com.sudo.railo.booking.application.dto.projection.SeatReservationProjection;
import com.sudo.railo.booking.application.dto.request.ReservationCreateRequest;
import com.sudo.railo.booking.application.dto.request.ReservationDeleteRequest;
import com.sudo.railo.booking.application.dto.response.ReservationDetail;
import com.sudo.railo.booking.application.dto.response.SeatReservationDetail;
import com.sudo.railo.booking.config.BookingConfig;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.status.ReservationStatus;
import com.sudo.railo.booking.domain.type.PassengerSummary;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepositoryCustom;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.train.domain.ScheduleStop;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.domain.status.OperationStatus;
import com.sudo.railo.train.domain.type.CarType;
import com.sudo.railo.train.exception.TrainErrorCode;
import com.sudo.railo.train.infrastructure.ScheduleStopRepository;
import com.sudo.railo.train.infrastructure.SeatRepository;
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
	private final ScheduleStopRepository scheduleStopRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationRepositoryCustom reservationRepositoryCustom;
	private final SeatRepository seatRepository;

	/**
	 * 예약을 생성하는 메서드
	 * @param request 예약 생성 요청 DTO
	 * @return 예약 레코드
	 */
	@Transactional
	public Reservation createReservation(ReservationCreateRequest request, String memberNo) {
		TrainSchedule trainSchedule = getTrainSchedule(request);
		Member member = memberRepository.getMember(memberNo);
		ScheduleStop departureStop = getStopStation(trainSchedule, request.departureStationId());
		ScheduleStop arrivalStop = getStopStation(trainSchedule, request.arrivalStationId());
		CarType carType = findCarType(request.seatIds());
		BigDecimal totalFare = getTotalFare(request, carType);

		validateTrainOperating(trainSchedule);

		Reservation reservation = generateReservation(
			request, trainSchedule, member, departureStop, arrivalStop, totalFare
		);
		return reservationRepository.save(reservation);
	}

	/**
	 * 예약을 조회하는 메서드
	 * @param memberNo 회원 번호
	 * @param reservationId 예약 ID
	 * @return 예약
	 */
	@Transactional
	public ReservationDetail getReservation(String memberNo, Long reservationId) {
		Member member = memberRepository.getMember(memberNo);

		List<ReservationInfo> reservationInfos = reservationRepositoryCustom.findReservationDetail(
			member.getId(), List.of(reservationId));

		if (reservationInfos.isEmpty()) {
			throw new BusinessException(BookingError.RESERVATION_NOT_FOUND);
		}

		ReservationInfo reservationInfo = reservationInfos.get(0);

		// 만료된 예약이면 삭제 처리
		LocalDateTime now = LocalDateTime.now();
		if (isExpired(reservationInfo, now)) {
			deleteReservation(reservationId);
			throw new BusinessException(BookingError.RESERVATION_EXPIRED);
		}

		return convertToReservationDetail(reservationInfo);
	}

	/**
	 * 예약 목록을 조회하는 메서드
	 * @param memberNo 회원 번호
	 * @return 예약 목록
	 */
	@Transactional
	public List<ReservationDetail> getReservations(String memberNo) {
		Member member = memberRepository.getMember(memberNo);

		// 예약 조회
		List<ReservationInfo> reservationInfos = reservationRepositoryCustom.findReservationDetail(member.getId());

		// 만료된 예약이면 삭제 처리
		LocalDateTime now = LocalDateTime.now();
		List<Long> expiredReservationIds = new ArrayList<>();
		List<ReservationInfo> validReservations = reservationInfos.stream()
			.filter(info -> {
				if (isExpired(info, now)) {
					expiredReservationIds.add(info.reservationId());
					return false;
				}
				return true;
			})
			.toList();

		if (!expiredReservationIds.isEmpty()) {
			deleteReservation(expiredReservationIds);
		}

		return convertToReservationDetail(validReservations);
	}

	/**
	 * 특정 예약을 삭제하는 메서드 - DTO 사용
	 * @param request 예약 삭제 요청 DTO
	 */
	@Transactional
	public void deleteReservation(ReservationDeleteRequest request) {
		try {
			deleteReservation(request.reservationId());
		} catch (Exception e) {
			throw new BusinessException(BookingError.RESERVATION_DELETE_FAILED);
		}
	}

	/**
	 * 특정 예약을 삭제하는 메서드 - 단수 예약 ID 사용
	 * @param reservationId 삭제할 예약의 ID
	 */
	private void deleteReservation(Long reservationId) {
		reservationRepository.deleteById(reservationId);
	}

	/**
	 * 다수의 예약을 삭제하는 메서드 - 복수 예약 ID 사용
	 * @param reservationIds 삭제할 예약의 ID를 원소로 하는 리스트
	 */
	private void deleteReservation(List<Long> reservationIds) {
		reservationRepository.deleteAllByIdInBatch(reservationIds);
	}

	/**
	 * 만료된 예약을 일괄삭제하는 메서드
	 */
	@Transactional
	public void expireReservations() {
		LocalDateTime now = LocalDateTime.now();
		int pageNumber = 0;
		final int pageSize = 500;
		Page<Reservation> expiredPage;
		do {
			Pageable pageable = PageRequest.of(pageNumber, pageSize);
			expiredPage = reservationRepository
				.findAllByExpiresAtBeforeAndReservationStatus(now, ReservationStatus.RESERVED, pageable);
			if (expiredPage.hasContent()) {
				List<Long> expiredList = expiredPage.getContent()
					.stream()
					.map(Reservation::getId)
					.toList();
				reservationRepository.deleteAllByIdInBatch(expiredList);
			}
			pageNumber++;
		} while (expiredPage.hasNext());
	}

	/**
	 * 예약 정보와 주어진 시간을 기준으로 예약이 만료되었는지 판단하는 메서드
	 * @param reservationInfo 예약 정보
	 * @param now 판단 기준이 될 시간
	 * @return 만료 여부
	 */
	private boolean isExpired(ReservationInfo reservationInfo, LocalDateTime now) {
		return reservationInfo.expiresAt().isBefore(now);
	}

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

	private ScheduleStop getStopStation(TrainSchedule trainSchedule, Long request) {
		return scheduleStopRepository.findByTrainScheduleIdAndStationId(trainSchedule.getId(), request)
			.orElseThrow(() -> new BusinessException(TrainErrorCode.STATION_NOT_FOUND));
	}

	private TrainSchedule getTrainSchedule(ReservationCreateRequest request) {
		return trainScheduleRepository.findById(request.trainScheduleId())
			.orElseThrow(() -> new BusinessException(TrainErrorCode.TRAIN_SCHEDULE_NOT_FOUND));
	}

	private BigDecimal getTotalFare(ReservationCreateRequest request, CarType carType) {
		return fareCalculationService.calculateFare(
			request.departureStationId(),
			request.arrivalStationId(),
			request.passengers(),
			carType
		);
	}

	/**
	 * 객차 타입 조회
	 */
	private CarType findCarType(List<Long> seatIds) {
		List<CarType> carTypes = seatRepository.findCarTypes(seatIds);

		// 입석 체크
		if (seatIds.isEmpty() && carTypes.isEmpty()) {
			return CarType.STANDARD;
		}

		if (carTypes.size() != 1) {
			throw new BusinessException(BookingError.INVALID_CAR_TYPE);
		}
		return carTypes.get(0);
	}

	private static void validateTrainOperating(TrainSchedule trainSchedule) {
		if (trainSchedule.getOperationStatus() == OperationStatus.CANCELLED) {
			throw new BusinessException(TrainErrorCode.TRAIN_OPERATION_CANCELLED);
		}
	}

	public List<ReservationDetail> convertToReservationDetail(List<ReservationInfo> reservationInfos) {
		return reservationInfos.stream()
			.map(this::convertToReservationDetail)
			.toList();
	}

	public ReservationDetail convertToReservationDetail(ReservationInfo reservationInfo) {
		return ReservationDetail.of(
			reservationInfo.reservationId(),
			reservationInfo.reservationCode(),
			String.format("%03d", reservationInfo.trainNumber()),
			reservationInfo.trainName(),
			reservationInfo.departureStationName(),
			reservationInfo.arrivalStationName(),
			reservationInfo.departureTime(),
			reservationInfo.arrivalTime(),
			reservationInfo.operationDate(),
			reservationInfo.expiresAt(),
			reservationInfo.fare(),
			convertToSeatReservationDetail(reservationInfo.seats())
		);
	}

	private List<SeatReservationDetail> convertToSeatReservationDetail(List<SeatReservationProjection> projection) {
		return projection.stream()
			.map(p -> SeatReservationDetail.of(
				p.getSeatReservationId(),
				p.getPassengerType(),
				p.getCarNumber(),
				p.getCarType(),
				p.getSeatNumber()
			))
			.toList();
	}

	private Reservation generateReservation(
		ReservationCreateRequest request,
		TrainSchedule trainSchedule,
		Member member,
		ScheduleStop departureStop,
		ScheduleStop arrivalStop,
		BigDecimal totalFare
	) {
		return Reservation.builder()
			.trainSchedule(trainSchedule)
			.member(member)
			.reservationCode(generateReservationCode())
			.tripType(request.tripType())
			.totalPassengers(request.passengers().stream().mapToInt(PassengerSummary::getCount).sum())
			.passengerSummary(convertPassengersToJson(request))
			.reservationStatus(ReservationStatus.RESERVED)
			.expiresAt(LocalDateTime.now().plusMinutes(bookingConfig.getExpiration().getReservation()))
			.fare(totalFare.intValue())
			.departureStop(departureStop)
			.arrivalStop(arrivalStop)
			.build();
	}

	private String convertPassengersToJson(ReservationCreateRequest request) {
		try {
			return objectMapper.writeValueAsString(request.passengers());
		} catch (JsonProcessingException e) {
			throw new BusinessException(BookingError.RESERVATION_CREATE_FAILED);
		}
	}

	public void deleteAllByMemberId(Long memberId) {
		reservationRepository.deleteAllByMemberId(memberId);
	}
}
