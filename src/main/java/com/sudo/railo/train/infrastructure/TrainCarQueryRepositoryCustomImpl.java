package com.sudo.railo.train.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.booking.domain.QSeatReservation;
import com.sudo.railo.booking.domain.SeatStatus;
import com.sudo.railo.train.application.dto.projection.QTrainCarProjection;
import com.sudo.railo.train.application.dto.projection.TrainCarProjection;
import com.sudo.railo.train.application.dto.response.TrainCarInfo;
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
			.join(t.trainCars, tc)
			.where(ts.id.eq(trainScheduleId))
			.orderBy(tc.carNumber.asc())
			.fetch();

		// 2. 각 객차별 예약된 좌석 수 계산
		Map<Long, Long> occupiedSeatsPerCar = queryFactory
			.select(tc.id, sr.count())
			.from(sr)
			.join(sr.seat, s)
			.join(s.trainCar, tc)
			.where(
				sr.trainSchedule.id.eq(trainScheduleId), // trainSchedule 직접 참조
				sr.seatStatus.in(SeatStatus.RESERVED, SeatStatus.LOCKED),
				sr.isStanding.isFalse(),
				// 구간 겹침 조건
				sr.departureStation.id.lt(arrivalStationId)
					.and(sr.arrivalStation.id.gt(departureStationId))
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
