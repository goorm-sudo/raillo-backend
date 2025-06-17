package com.sudo.railo.ticket.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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
    private String vendorCode;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 일자 (MMdd)'")
    private String purchaseDate;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 결제 순번 (10000~)'")
    private String purchaseSeq;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) COMMENT '승차권 고유번호 (2자리)'")
    private String purchaseUid;

}
