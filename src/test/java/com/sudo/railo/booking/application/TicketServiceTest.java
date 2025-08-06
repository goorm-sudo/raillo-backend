package com.sudo.railo.booking.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sudo.railo.booking.application.dto.response.TicketReadResponse;
import com.sudo.railo.booking.domain.Qr;
import com.sudo.railo.booking.domain.Reservation;
import com.sudo.railo.booking.domain.Ticket;
import com.sudo.railo.booking.domain.status.ReservationStatus;
import com.sudo.railo.booking.domain.status.TicketStatus;
import com.sudo.railo.booking.domain.type.PassengerType;
import com.sudo.railo.booking.domain.type.TripType;
import com.sudo.railo.booking.infrastructure.QrRepository;
import com.sudo.railo.booking.infrastructure.reservation.ReservationRepository;
import com.sudo.railo.booking.infrastructure.ticket.TicketRepository;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;
import com.sudo.railo.support.helper.TrainScheduleTestHelper;
import com.sudo.railo.support.helper.TrainTestHelper;
import com.sudo.railo.train.domain.Seat;
import com.sudo.railo.train.domain.Train;
import com.sudo.railo.train.domain.type.CarType;

import lombok.extern.slf4j.Slf4j;

@ServiceTest
@Slf4j
class TicketServiceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private QrRepository qrRepository;

	@Autowired
	private TicketService ticketService;

	@Autowired
	private TrainTestHelper trainTestHelper;

	@Autowired
	private TrainScheduleTestHelper trainScheduleTestHelper;

	private Reservation reservation;
	private Seat seat1, seat2;
	private PassengerType passengerType1, passengerType2;

	@BeforeEach
	void setup() {
		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);
		Train train = trainTestHelper.createKTX();
		TrainScheduleTestHelper.TrainScheduleWithStopStations schedule = trainScheduleTestHelper.createSchedule(train);
		Reservation reservation = Reservation.builder()
			.trainSchedule(schedule.trainSchedule())
			.member(member)
			.reservationCode("20250806100001D49J")
			.tripType(TripType.OW)
			.totalPassengers(1)
			.passengerSummary("[{\"passengerType\":\"CHILD\",\"count\":1},{\"passengerType\":\"VETERAN\",\"count\":1}]")
			.reservationStatus(ReservationStatus.RESERVED)
			.expiresAt(LocalDateTime.now().plusMinutes(10))
			.fare(50000)
			.departureStop(schedule.scheduleStops().get(0))
			.arrivalStop(schedule.scheduleStops().get(1))
			.build();
		this.reservation = reservationRepository.save(reservation);
		List<Seat> seats = trainTestHelper.getSeats(train, CarType.STANDARD, 2);
		seat1 = seats.get(0);
		seat2 = seats.get(1);

		passengerType1 = PassengerType.CHILD;
		passengerType2 = PassengerType.VETERAN;
	}

	@Test
	@DisplayName("예약, 좌석, 승객 유형으로 티켓 생성에 성공한다")
	void reservationAndSeatAndPassengerType_createTicket_success() {
		// when
		ticketService.createTicket(reservation, seat1, passengerType1);

		// then
		List<Ticket> result = ticketRepository.findAll();
		assertThat(result.size()).isEqualTo(1);
		Ticket resultItem = result.get(0);
		assertThat(resultItem.getTicketStatus()).isEqualTo(TicketStatus.ISSUED);
	}

	@Test
	@DisplayName("멤버번호로 가지고 있는 티켓 조회에 성공한다")
	void memberNo_getMyTickets_success() {
		// given
		Qr qr1 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());
		Qr qr2 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());

		Ticket ticket1 = Ticket.builder()
			.seat(seat1)
			.reservation(reservation)
			.qr(qr1)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType1)
			.build();
		ticketRepository.save(ticket1);

		Ticket ticket2 = Ticket.builder()
			.seat(seat2)
			.reservation(reservation)
			.qr(qr2)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType2)
			.build();
		ticketRepository.save(ticket2);
		String memberNo = reservation.getMember().getMemberDetail().getMemberNo();

		// when
		List<TicketReadResponse> result = ticketService.getMyTickets(memberNo);

		// then
		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	@DisplayName("티켓 ID로 티켓 삭제에 성공한다")
	void ticketId_deleteTicket_success() {
		// given
		Qr qr1 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());
		Qr qr2 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());

		Ticket ticket1 = Ticket.builder()
			.seat(seat1)
			.reservation(reservation)
			.qr(qr1)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType1)
			.build();
		Ticket entity1 = ticketRepository.save(ticket1);

		Ticket ticket2 = Ticket.builder()
			.seat(seat2)
			.reservation(reservation)
			.qr(qr2)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType2)
			.build();
		Ticket entity2 = ticketRepository.save(ticket2);

		// when
		ticketService.deleteTicketById(entity1.getId());

		// then
		List<Ticket> result = ticketRepository.findAll();
		assertThat(result.size()).isEqualTo(1);
		Ticket resultItem = result.get(0);
		assertThat(resultItem.getPassengerType()).isEqualTo(entity2.getPassengerType());
	}

	@Test
	@DisplayName("예약 ID로 티켓 삭제에 성공한다")
	void reservationId_deleteTicket_success() {
		// given
		Qr qr1 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());
		Qr qr2 = qrRepository.save(Qr.builder().isUsable(true).scanCount(0).build());

		Ticket ticket1 = Ticket.builder()
			.seat(seat1)
			.reservation(reservation)
			.qr(qr1)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType1)
			.build();
		ticketRepository.save(ticket1);

		Ticket ticket2 = Ticket.builder()
			.seat(seat2)
			.reservation(reservation)
			.qr(qr2)
			.ticketStatus(TicketStatus.ISSUED)
			.passengerType(passengerType2)
			.build();
		ticketRepository.save(ticket2);

		// when
		ticketService.deleteTicketByReservationId(reservation.getId());

		// then
		List<Ticket> result = ticketRepository.findAll();
		assertThat(result.size()).isEqualTo(0);
	}
}
