package com.sudo.railo.booking.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ticket_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "qr_id", unique = true)
	private Qr qr;

	@CreatedDate
	@Column(updatable = false, nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PassengerType passengerType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus paymentStatus;

	private LocalDateTime paymentAt;

	@Enumerated(EnumType.STRING)
	private TicketStatus status;

	@Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 발행 주체 코드 (웹, 모바일, 역 등 - 5자리)'")
	private String wctNumber;

	@Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 일자 (MMdd)'")
	private String saleAt;

	@Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 순번 (10000~)'")
	private String saleSeqNumber;

	@Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 고유번호 (2자리)'")
	private String pwd;

}
