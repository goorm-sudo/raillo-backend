package com.sudo.railo.train.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Train {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int trainNumber;

    private String trainType;

    private String trainName;

    private int totalCars;
}
