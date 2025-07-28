package com.sudo.railo.member.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteExpiredMembersScheduler {

	private final JobLauncher jobLauncher;
	private final Job deleteExpiredMembersJob;

	@Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 수행
	public void runDeleteExpiredMembers() {
		try {
			JobParameters jobParameters = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis()) // 작업 식별을 위한 시간 파라미터 추가
				.toJobParameters();

			jobLauncher.run(deleteExpiredMembersJob, jobParameters);

		} catch (JobExecutionAlreadyRunningException | JobRestartException
				 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
			log.error("회원 영구 삭제 배치 작업 실행 중 오류 발생", e);
		}

	}
}
