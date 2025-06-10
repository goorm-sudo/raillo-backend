package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationFare {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int standardFare;

    private int firstClassFare;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id")
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id")
    private Station arrivalStation;
}
