package com.sudo.railo.train.infrastructure;

import static com.sudo.railo.booking.domain.QReservation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.QSeatReservation;
import com.sudo.railo.train.application.dto.projection.QTrainCarProjection;
import com.sudo.railo.train.application.dto.projection.TrainCarProjection;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
import com.sudo.railo.train.domain.QScheduleStop;
import com.sudo.railo.train.domain.QSeat;
import com.sudo.railo.train.domain.QTrain;
import com.sudo.railo.train.domain.QTrainCar;
import com.sudo.railo.train.domain.QTrainSchedule;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TrainCarQueryRepositoryCustomImpl implements TrainCarQueryRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	/**
	 * 열차 객차 목록 조회 (잔여 좌석이 있는 객차만)
	 */
	@Override
	public List<TrainCarInfo> findAvailableTrainCars(Long trainScheduleId, Long departureStationId,
		Long arrivalStationId) {
		QTrainSchedule ts = QTrainSchedule.trainSchedule;
		QTrain t = QTrain.train;
		QTrainCar tc = QTrainCar.trainCar;
		QSeat s = QSeat.seat;
		QSeatReservation sr = QSeatReservation.seatReservation;

		// stopOrder 기반 구간 겹침을 위한 ScheduleStop 조인
		QScheduleStop reservedDepartureStop = new QScheduleStop("reservedDepartureStop");
		QScheduleStop reservedArrivalStop = new QScheduleStop("reservedArrivalStop");
		QScheduleStop searchDepartureStop = new QScheduleStop("searchDepartureStop");
		QScheduleStop searchArrivalStop = new QScheduleStop("searchArrivalStop");

		// 1. 해당 trainScheduleId의 객차(trainCar) 조회
		List<TrainCarProjection> carProjections = queryFactory
			.select(new QTrainCarProjection(
				tc.id,
				tc.carNumber,
				tc.carType,
				tc.totalSeats,
				Expressions.constant(0), // 임시 remainingSeats 기본값 처리
				tc.seatArrangement
			))
			.from(ts)
			.join(ts.train, t)
			.join(tc).on(tc.train.id.eq(t.id))
			.where(ts.id.eq(trainScheduleId))
			.orderBy(tc.carNumber.asc())
			.fetch();

		// 2. 각 객차별 예약된 좌석 수 계산
		Map<Long, Long> occupiedSeatsPerCar = queryFactory
			.select(tc.id, sr.count())
			.from(sr)
			.join(sr.seat, s)
			.join(s.trainCar, tc)
			.join(sr.reservation, reservation)
			// stopOrder 기반 구간 겹침 조건
			.join(reservation.departureStop, reservedDepartureStop)
			.join(reservation.arrivalStop, reservedArrivalStop)
			.join(searchDepartureStop).on(
				searchDepartureStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchDepartureStop.station.id.eq(departureStationId))
			)
			.join(searchArrivalStop).on(
				searchArrivalStop.trainSchedule.id.eq(trainScheduleId)
					.and(searchArrivalStop.station.id.eq(arrivalStationId))
			)
			.where(
				sr.trainSchedule.id.eq(trainScheduleId),
				sr.seat.isNotNull(),
				// stopOrder 기반 구간 겹침 조건
				// 구간 겹침: NOT(예약종료 <= 검색시작 OR 예약시작 >= 검색종료)
				// = 예약종료 > 검색시작 AND 예약시작 < 검색종료
				reservedArrivalStop.stopOrder.gt(searchDepartureStop.stopOrder)
					.and(reservedDepartureStop.stopOrder.lt(searchArrivalStop.stopOrder))
			)
			.groupBy(tc.id)
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(tc.id),
				tuple -> tuple.get(sr.count())
			));

		// 3. remainingSeats 계산하여 업데이트하고 응답용 record로 변환
		return carProjections.stream()
			.map(projection -> {
				long occupiedSeats = occupiedSeatsPerCar.getOrDefault(projection.getId(), 0L);
				int remainingSeats = Math.max(0, projection.getTotalSeats() - (int)occupiedSeats);
				return projection.withRemainingSeats(remainingSeats);
			})
			.filter(projection -> projection.getRemainingSeats() > 0) // 잔여 좌석이 있는 객차만
			.map(TrainCarProjection::toTrainCarInfo) // 응답용 record로 변환
			.toList();
	}
}
