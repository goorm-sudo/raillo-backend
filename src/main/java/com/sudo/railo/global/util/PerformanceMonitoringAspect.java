package com.sudo.railo.global.util;

import java.lang.reflect.Method;
import java.util.Collection;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 성능 모니터링 AOP
 *
 * N+1 분석 흐름:
 * 1. @MonitorPerformance → 전체 모니터링 시작, 쿼리 카운터 초기화
 * 2. @TrackQuery → 각 Repository 메서드 실행시마다 카운터 증가
 * 3. @MonitorPerformance → 전체 종료시 총 쿼리/처리건수로 N+1 판정
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {

	/**
	 * 메인 성능 모니터링 (Controller나 Service 메서드용)
	 *
	 * 흐름:
	 * 1. PerformanceMonitor.startMonitoring() → 쿼리 카운터를 0으로 초기화
	 * 2. joinPoint.proceed() → 실제 메서드 실행 (이 안에서 @TrackQuery들이 카운터 증가)
	 * 3. analyzePerformance() → 총 쿼리 개수와 처리 건수를 비교해 N+1 판정
	 * 4. PerformanceMonitor.printAndClear() → 상세 쿼리 통계 출력 후 정리
	 */
	@Around("@annotation(monitorPerformance)")
	public Object monitorPerformance(ProceedingJoinPoint joinPoint, MonitorPerformance monitorPerformance) throws
		Throwable {
		String methodName = joinPoint.getSignature().getName();
		String description = monitorPerformance.value().isEmpty() ? methodName : monitorPerformance.value();

		log.info("=== {} 성능 모니터링 시작 ===", description);

		// 모니터링 시작 → 쿼리 카운터 초기화
		PerformanceMonitor.startMonitoring();
		long startTime = System.currentTimeMillis();

		try {
			// 실제 메서드 실행 → 이 안에서 @TrackQuery가 붙은 메서드들이 카운터 증가
			Object result = joinPoint.proceed();

			long endTime = System.currentTimeMillis();
			long executionTime = endTime - startTime;

			// 결과 분석 → N+1 감지 로직 실행
			analyzePerformance(description, result, executionTime, monitorPerformance.enableN1Detection());

			return result;

		} catch (Exception e) {
			log.error("=== {} 실행 중 오류 발생 ===", description, e);
			throw e;
		} finally {
			// 통계 출력 및 정리 → 쿼리별 상세 통계와 총합 출력
			PerformanceMonitor.printAndClear();
		}
	}

	/**
	 * 개별 쿼리 추적 (Repository 메서드용)
	 *
	 * 흐름:
	 * 1. 쿼리 실행 전 시간 기록
	 * 2. joinPoint.proceed() → 실제 Repository 메서드 (DB 쿼리) 실행
	 * 3. PerformanceMonitor.recordQuery() → 쿼리 이름, 실행시간 기록 + 카운터 1 증가
	 * 4. logQueryResult() → 개별 쿼리 결과 로깅 (건수, 실행시간)
	 */
	@Around("@annotation(trackQuery)")
	public Object trackQuery(ProceedingJoinPoint joinPoint, TrackQuery trackQuery) throws Throwable {
		long startTime = System.currentTimeMillis();

		try {
			// 실제 Repository 메서드 (DB 쿼리) 실행
			Object result = joinPoint.proceed();

			long endTime = System.currentTimeMillis();
			long executionTime = endTime - startTime;

			// 쿼리 실행 시간 기록 + 전체 쿼리 카운터 증가
			PerformanceMonitor.recordQuery(trackQuery.queryName(), executionTime);

			// 개별 쿼리 결과 로깅
			logQueryResult(trackQuery.queryName(), result, executionTime);

			return result;

		} catch (Exception e) {
			log.error("쿼리 {} 실행 중 오류: {}", trackQuery.queryName(), e.getMessage());
			throw e;
		}
	}

	/**
	 * 성능 분석 결과 출력
	 *
	 * 흐름:
	 * 1. analyzeResultSize() → 처리된 데이터 건수 파악 (ex: 10개 열차)
	 * 2. analyzeN1Problem() → 총 쿼리 개수를 데이터 건수로 나누어 N+1 판정
	 *    예: 41개 쿼리 ÷ 10개 열차 = 4.1개/건 → N+1 문제!
	 */
	private void analyzePerformance(String description, Object result, long executionTime, boolean enableN1Detection) {
		log.info("=== {} 성능 분석 결과 ===", description);
		log.info("전체 실행시간: {}ms", executionTime);

		// 처리된 데이터 건수 분석
		if (result != null) {
			analyzeResultSize(result);
		}

		// N+1 문제 분석 → 쿼리 개수 vs 데이터 건수 비율 계산
		if (enableN1Detection) {
			int totalQueries = PerformanceMonitor.getQueryCount();
			analyzeN1Problem(totalQueries, result);
		}

		log.info("==========================================");
	}

	/**
	 * 결과 크기 분석 - 처리된 데이터 건수 파악
	 *
	 * 흐름:
	 * 1. 반환 객체가 Collection이면 size() 호출
	 * 2. TrainSearchSlicePageResponse면 리플렉션으로 getContent().size() 호출
	 * 3. 로그로 "처리된 데이터 건수: X건" 출력
	 */
	private void analyzeResultSize(Object result) {
		if (result instanceof Collection<?> collection) {
			log.info("처리된 데이터 건수: {}", collection.size());
		} else if (result.getClass().getSimpleName().contains("SlicePageResponse")) {
			// TrainSearchSlicePageResponse의 경우
			try {
				Method getContentMethod = result.getClass().getMethod("getContent");
				Object content = getContentMethod.invoke(result);
				if (content instanceof Collection<?> collection) {
					log.info("처리된 데이터 건수: {}", collection.size());
				}
			} catch (Exception e) {
				log.debug("결과 크기 분석 실패: {}", e.getMessage());
			}
		}
	}

	/**
	 * N+1 문제 분석 - 쿼리 개수와 데이터 건수 비율로 N+1 판정
	 *
	 * 흐름:
	 * 1. totalQueries (예: 41개) ÷ itemCount (예: 10개) = queriesPerItem (예: 4.1개/건)
	 * 2. queriesPerItem > 3 → "🚨 N+1 문제 의심!"
	 * 3. queriesPerItem > 1 → "⚠️ 다소 많은 쿼리"
	 * 4. 단일 결과인 경우 totalQueries > 5 → "🚨 쿼리 과다 실행"
	 */
	private void analyzeN1Problem(int totalQueries, Object result) {
		if (result instanceof Collection<?> collection) {
			int itemCount = collection.size();
			if (itemCount > 0) {
				double queriesPerItem = (double)totalQueries / itemCount;
				log.info("아이템당 평균 쿼리 수: {:.2f}", queriesPerItem);

				if (queriesPerItem > 3) {
					log.warn("🚨 N+1 문제 의심! 아이템 {}건 처리에 {}개 쿼리 사용", itemCount, totalQueries);
				} else if (queriesPerItem > 1) {
					log.warn("⚠️ 다소 많은 쿼리: 아이템 {}건 처리에 {}개 쿼리 사용", itemCount, totalQueries);
				}
			}
		} else {
			// 단일 결과의 경우
			if (totalQueries > 5) {
				log.warn("🚨 쿼리 과다 실행: 단일 결과 처리에 {}개 쿼리 사용", totalQueries);
			}
		}
	}

	/**
	 * 쿼리 결과 로깅 - 개별 쿼리의 실행 결과 기록
	 *
	 * 흐름:
	 * 1. Collection이면 "쿼리명: X건 조회, Yms" 로깅
	 * 2. Number면 "쿼리명: 결과값 X, Yms" 로깅
	 * 3. 기타는 "쿼리명: 실행완료, Yms" 로깅
	 * 4. 실행시간 > 100ms면 "🐌 느린 쿼리 감지" 경고
	 */
	private void logQueryResult(String queryName, Object result, long executionTime) {
		if (result instanceof Collection<?> collection) {
			log.debug("{}: {}건 조회, {}ms", queryName, collection.size(), executionTime);
		} else if (result instanceof Number number) {
			log.debug("{}: 결과값 {}, {}ms", queryName, number, executionTime);
		} else {
			log.debug("{}: 실행완료, {}ms", queryName, executionTime);
		}

		// 느린 쿼리 경고
		if (executionTime > 100) {
			log.warn("🐌 느린 쿼리 감지: {} - {}ms", queryName, executionTime);
		}
	}
}
