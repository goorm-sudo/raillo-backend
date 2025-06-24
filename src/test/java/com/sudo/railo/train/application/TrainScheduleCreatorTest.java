package com.sudo.railo.train.application;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrainScheduleCreatorTest {

	@Autowired
	private TrainScheduleCreator creator;

	@Test
	void createOneMonthTrainSchedule() {
		LocalDate startDate = LocalDate.now();
		LocalDate endDate = startDate.plusMonths(1);

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			creator.createTrainSchedule(date);
		}
	}

	@Test
	void createTodayTrainSchedule() {
		creator.createTrainSchedule(LocalDate.now());
	}
}
