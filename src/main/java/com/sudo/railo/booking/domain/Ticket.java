package com.sudo.railo.booking.domain;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.sudo.railo.global.domain.BaseEntity;
import com.sudo.railo.train.domain.Seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ticket_id")
	private Long id; // 승차권 ID

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id")
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private Seat seat; // 좌석 ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Reservation reservation; // 예약 ID

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "qr_id", nullable = false)
	private Qr qr; // QR ID

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TicketStatus ticketStatus; // 승차권 상태

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PassengerType passengerType; // 승객 유형

	@Column(nullable = false)
	private int fare; // 운임

	@Column(nullable = true, columnDefinition = "VARCHAR(255) COMMENT '승차권 발행 주체 코드 (웹, 모바일, 역 등 - 5자리)'")
	private String vendorCode; // 결제 위치 번호 (예: 온라인 (01), ~~역(02...))

	@Column(nullable = true, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 일자 (MMdd)'")
	private String purchaseDate; // 결제 날짜 (MMdd)

	@Column(nullable = true, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 순번 (10000~)'")
	private String purchaseSeq; // 결제 순번 (10000~)

	@Column(nullable = true, columnDefinition = "VARCHAR(255) COMMENT '승차권 고유번호 (2자리)'")
	private String purchaseUid; // 결제 고유번호 (숫자 2자리)
}
