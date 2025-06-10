package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainCar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int carNumber;

    @Enumerated(EnumType.STRING)
    private CarType carType;

    @Enumerated(EnumType.STRING)
    private CarTypeName carTypeName;

    private int totalSeat;

    private String seatArrangement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id")
    private Train train;
}
