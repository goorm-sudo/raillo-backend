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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TicketType ticketType;

    @Column(nullable = false)
    private LocalDate StartAt;

    private LocalDate EndAt;

    @Column(nullable = false, length = 1)
    private String isHolidayUsable;

    @Enumerated(EnumType.STRING)
    private SeasonStatus seasonStatus;

    @OneToOne
    @JoinColumn(name = "qr_id", unique = true)
    private Qr qr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
