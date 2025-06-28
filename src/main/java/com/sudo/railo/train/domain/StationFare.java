package com.sudo.railo.train.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StationFare {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "station_fare_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_station_id")
	private Station departureStation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_station_id")
	private Station arrivalStation;

	private int standardFare;

	private int firstClassFare;

	private StationFare(Station departureStation, Station arrivalStation, int standardFare, int firstClassFare) {
		this.departureStation = departureStation;
		this.arrivalStation = arrivalStation;
		this.standardFare = standardFare;
		this.firstClassFare = firstClassFare;
	}

	public static StationFare create(Station departureStation, Station arrivalStation,
		int standardFare, int firstClassFare) {

		return new StationFare(departureStation, arrivalStation, standardFare, firstClassFare);
	}
}
