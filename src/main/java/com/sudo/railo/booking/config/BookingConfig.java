package com.sudo.railo.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "booking")
public class BookingConfig {

	private final Expiration expiration;

	@Getter
	@AllArgsConstructor
	public static class Expiration {
		private final int reservation;
	}
}
