package com.sudo.railo.global.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 성능 통계 관리 클래스
 *
 * ThreadLocal로 쓰레드별 쿼리 통계 수집, N+1 감지용 카운터 관리
 * - ThreadLocal로 쓰레드 안전성 보장
 * - 쿼리별 실행 횟수, 시간 통계 수집
 */
@Component
@Slf4j
public class PerformanceMonitor {

	/**
	 * 쿼리 통계 저장 클래스
	 * queryTimes: 쿼리명별 실행시간 리스트, totalQueries: 총 쿼리 개수
	 */
	public static class QueryStats {
		private final Map<String, List<Long>> queryTimes = new HashMap<>();
		private final AtomicInteger totalQueries = new AtomicInteger(0);

		/**
		 * 쿼리 실행 기록 추가 - queryTimes에 시간 추가, totalQueries 카운터 증가
		 */
		public void addQuery(String queryName, long executionTime) {
			queryTimes.computeIfAbsent(queryName, k -> new ArrayList<>()).add(executionTime);
			totalQueries.incrementAndGet();
		}

		/**
		 * 수집된 통계 출력 - 총 쿼리 개수, 각 쿼리별 실행 횟수/평균/최대/최소 시간
		 */
		public void printStats() {
			log.info("=== 상세 쿼리 성능 통계 ===");
			log.info("총 쿼리 실행 횟수: {}", totalQueries.get());

			queryTimes.forEach((queryName, times) -> {
				if (!times.isEmpty()) {
					double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
					long maxTime = times.stream().mapToLong(Long::longValue).max().orElse(0);
					long minTime = times.stream().mapToLong(Long::longValue).min().orElse(0);

					// 소수점 반올림해서 정수로 표시
					int avgTimeRounded = (int)Math.round(avgTime);

					log.info("{}를 실행 {}회, 평균 {}ms, 최소 {}ms, 최대 {}ms",
						queryName, times.size(), avgTimeRounded, minTime, maxTime);
				}
			});
		}
	}

	// ThreadLocal로 쓰레드별 독립적인 통계 관리
	private static final ThreadLocal<QueryStats> stats = new ThreadLocal<>();
	private static final ThreadLocal<Integer> queryCount = new ThreadLocal<>();

	/**
	 * 모니터링 시작 - @MonitorPerformance 시작시 호출, QueryStats 생성 + 카운터 0 초기화
	 */
	public static void startMonitoring() {
		stats.set(new QueryStats());
		queryCount.set(0);
	}

	/**
	 * 쿼리 실행 기록 - @TrackQuery 실행시마다 호출, 통계에 쿼리명+시간 추가, 카운터++
	 */
	public static void recordQuery(String queryName, long executionTime) {
		QueryStats currentStats = stats.get();
		if (currentStats != null) {
			currentStats.addQuery(queryName, executionTime);
		}

		Integer count = queryCount.get();
		if (count != null) {
			queryCount.set(count + 1);
		}
	}

	/**
	 * 총 쿼리 개수 반환 - N+1 분석용, 공식: getQueryCount() ÷ 처리건수 = 평균 쿼리/건
	 */
	public static int getQueryCount() {
		return queryCount.get() != null ? queryCount.get() : 0;
	}

	/**
	 * 통계 출력 및 정리 - @MonitorPerformance 종료시 호출, printStats() 후 ThreadLocal 제거
	 */
	public static void printAndClear() {
		QueryStats currentStats = stats.get();
		if (currentStats != null) {
			currentStats.printStats();
		}
		stats.remove();
		queryCount.remove();
	}
}
