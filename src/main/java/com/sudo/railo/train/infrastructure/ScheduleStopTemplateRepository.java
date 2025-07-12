package com.sudo.railo.train.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.train.domain.ScheduleStopTemplate;

public interface ScheduleStopTemplateRepository extends JpaRepository<ScheduleStopTemplate, UUID> {
}
