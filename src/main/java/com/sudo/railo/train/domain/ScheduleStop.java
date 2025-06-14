package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleStop {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int stopOrder;

    private LocalTime arrivalTime;

    private LocalTime departureTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_schedule_id")
    private TrainSchedule trainSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;


}
