package com.sudo.railo.ticket.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seasonTicketId;

    @OneToOne
    @JoinColumn(name = "qr_id", unique = true)
    private Qr qrId;

    // TODO: 멤버 id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType ticketType;

    @Column(nullable = false)
    private LocalDate startAt;

    private LocalDate endAt;

    @Column(nullable = false)
    private boolean isHolidayUsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeasonTicketStatus seasonTicketStatus;

}
