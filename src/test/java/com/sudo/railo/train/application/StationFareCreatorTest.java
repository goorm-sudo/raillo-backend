package com.sudo.railo.train.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StationFareCreatorTest {

	@Autowired
	private StationFareCreator creator;

	@Test
	void createStationFare() {
		creator.createStationFare();
	}
}
