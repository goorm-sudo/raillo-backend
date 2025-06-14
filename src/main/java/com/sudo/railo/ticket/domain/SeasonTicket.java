package com.sudo.railo.ticket.domain;

import com.sudo.railo.member.domain.Member;
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
    @Column(name = "season_ticket_id")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "qr_id", unique = true)
    private Qr qr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

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
