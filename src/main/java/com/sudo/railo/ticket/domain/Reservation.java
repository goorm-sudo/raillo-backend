package com.sudo.railo.ticket.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    // TODO: 운행 일정 id

    // TODO: 멤버 id

    // TODO: 출발역 id

    // TODO: 도착역 id

    private Long reservationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripType tripType;

    private int totalPassengers;

    private String passengerSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus reservationStatus;

    private LocalDateTime expiresAt;

    private LocalDateTime reservedAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

}
