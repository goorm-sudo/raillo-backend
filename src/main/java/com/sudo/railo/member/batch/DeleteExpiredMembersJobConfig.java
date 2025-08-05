package com.sudo.railo.member.batch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.sudo.railo.member.infrastructure.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeleteExpiredMembersJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final MemberRepository memberRepository;
	private final DataSource dataSource;

	private static final int CHUNK_SIZE = 100; // 100개씩 처리

	@Bean
	public Job deleteExpiredMembersJob() {
		return new JobBuilder("deleteExpiredMembersJob", jobRepository)
			.start(deleteExpiredMembersStep())
			.build();
	}

	@Bean
	public Step deleteExpiredMembersStep() {
		return new StepBuilder("deleteExpiredMembersStep", jobRepository)
			.<Long, Long>chunk(CHUNK_SIZE, transactionManager)
			.reader(expiredMembersReader())
			.writer(expiredMembersWriter())
			.build();
	}

	@Bean
	public JdbcPagingItemReader<Long> expiredMembersReader() {

		MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
		queryProvider.setSelectClause("m.id");
		queryProvider.setFromClause("member m");
		queryProvider.setWhereClause("m.is_deleted = true AND m.updated_at < :date");

		Map<String, Order> sortKeys = new HashMap<>();
		sortKeys.put("id", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);

		// 삭제된지 3년이 지난 회원 조회 파라미터
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("date", LocalDateTime.now().minusYears(3));

		return new JdbcPagingItemReaderBuilder<Long>()
			.name("expiredMembersReader")
			.dataSource(dataSource)
			.queryProvider(queryProvider)
			.parameterValues(parameters)
			.rowMapper((rs, rowNum) -> rs.getLong("id"))
			.pageSize(CHUNK_SIZE)
			.build();
	}

	@Bean
	public ItemWriter<Long> expiredMembersWriter() {
		// 영구 삭제 처리
		return chunk -> {
			List<Long> memberIds = new ArrayList<>(chunk.getItems());

			if (!memberIds.isEmpty()) {
				log.info("총 {}명의 회원 영구 삭제 처리", memberIds.size());

				RetryTemplate retryTemplate = new RetryTemplate();
				SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5); // 최대 5번 재시도
				retryTemplate.setRetryPolicy(retryPolicy);

				// 백오프 정책 설정
				ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
				backOffPolicy.setInitialInterval(250);
				backOffPolicy.setMultiplier(2);
				backOffPolicy.setMaxInterval(1000); //최대 1초 대기
				retryTemplate.setBackOffPolicy(backOffPolicy);

				try {
					retryTemplate.execute(context -> {
						memberRepository.deleteAllByIdInBatch(memberIds);
						log.info("회원 영구 삭제 성공");
						return null;
					}, context -> {
						throw new RuntimeException("회원 삭제 실패: 최대 재시도 횟수 초과");
					});
				} catch (Exception e) {
					log.error("회원 영구 삭제 처리 중 오류 발생: {}", e.getMessage());
					throw e;
				}

			}
		};
	}

}
