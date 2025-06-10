package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int seatNumber;

    @Column(length = 1)
    private String seatRow;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    @Column(length = 1)
    private String isAccessible;

    @Column(length = 1)
    private String isAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_car_id")
    private TrainCar trainCar;
}
