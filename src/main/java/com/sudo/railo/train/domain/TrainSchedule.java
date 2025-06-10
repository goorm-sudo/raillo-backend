package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scheduleName;

    private LocalDate operationDate;

    private LocalTime departureTime;

    private LocalTime arrivalTime;

    @Enumerated(EnumType.STRING)
    private OperationStatus operationStatus;

    private int delayMinutes;

    private int totalSeats;

    private int availableSeats;

    @OneToOne
    @JoinColumn(name="train_id", unique = true)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id")
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id")
    private Station arrivalStation;
}
