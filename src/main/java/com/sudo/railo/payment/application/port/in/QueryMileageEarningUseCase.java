package com.sudo.railo.payment.application.port.in;

import com.sudo.railo.payment.domain.entity.MileageEarningSchedule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마일리지 적립 조회 유스케이스
 * 
 * 헥사고날 아키텍처의 입력 포트로, 마일리지 적립 스케줄 및 
 * 통계 정보를 조회하는 비즈니스 기능을 정의합니다.
 */
public interface QueryMileageEarningUseCase {
    
    /**
     * 회원의 적립 예정 마일리지를 조회합니다.
     * 
     * @param memberId 회원 ID
     * @return 적립 예정 마일리지 총액
     */
    BigDecimal getPendingMileageByMemberId(Long memberId);
    
    /**
     * 회원의 마일리지 적립 스케줄을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @param status 조회할 스케줄 상태 (null인 경우 전체 조회)
     * @return 마일리지 적립 스케줄 목록
     */
    List<MileageEarningSchedule> getEarningSchedulesByMemberId(
        Long memberId, 
        MileageEarningSchedule.EarningStatus status
    );
    
    /**
     * 특정 결제의 마일리지 적립 스케줄을 조회합니다.
     * 
     * @param paymentId 결제 ID
     * @return 마일리지 적립 스케줄 (없으면 Optional.empty())
     */
    Optional<MileageEarningSchedule> getEarningScheduleByPaymentId(String paymentId);
    
    /**
     * 마일리지 적립 통계를 조회합니다.
     * 
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 통계 정보 맵
     */
    Map<String, Object> getEarningStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 지연 보상 통계를 조회합니다.
     * 
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 지연 보상 통계 정보 맵
     */
    Map<String, Object> getDelayCompensationStatistics(LocalDateTime startTime, LocalDateTime endTime);
}