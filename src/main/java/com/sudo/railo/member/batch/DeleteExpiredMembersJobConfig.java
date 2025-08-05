package com.sudo.railo.member.batch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.infrastructure.MemberRepository;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeleteExpiredMembersJobConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final MemberRepository memberRepository;

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
			.<Member, Member>chunk(CHUNK_SIZE, transactionManager)
			.reader(expiredMembersReader())
			.writer(expiredMembersWriter())
			.build();
	}

	@Bean
	public JpaPagingItemReader<Member> expiredMembersReader() {

		JpaNativeQueryProvider<Member> queryProvider = new JpaNativeQueryProvider<>();
		queryProvider.setEntityClass(Member.class);

		queryProvider.setSqlQuery("SELECT * FROM member m WHERE m.is_deleted = true AND m.updated_at < :date");

		// 삭제된지 3년이 지난 회원 조회 파라미터
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("date", LocalDateTime.now().minusYears(3));

		// JPA 를 사용하여 페이징 방식으로 데이터 읽어옴
		return new JpaPagingItemReaderBuilder<Member>()
			.name("expiredMembersReader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(CHUNK_SIZE)
			.queryProvider(queryProvider)
			.parameterValues(parameters)
			.build();
	}

	@Bean
	public ItemWriter<Member> expiredMembersWriter() {
		// 영구 삭제 처리
		return chunk -> {
			List<Long> memberIds = chunk.getItems().stream()
				.map(Member::getId)
				.filter(Objects::nonNull)
				.toList();

			if (!memberIds.isEmpty()) {
				log.info("총 {}명의 회원 영구 삭제 처리", memberIds.size());

				memberRepository.deleteAllByIdInBatch(memberIds);
			}
		};
	}

}
