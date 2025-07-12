package com.sudo.railo.payment.application.port.out;

import java.time.LocalDateTime;

/**
 * 열차 스케줄 정보 조회 포트
 * 
 * 헥사고날 아키텍처의 출력 포트로, Train 도메인에서
 * 마일리지 적립에 필요한 열차 스케줄 정보를 조회하는 기능을 정의합니다.
 */
public interface LoadTrainSchedulePort {
    
    /**
     * 열차 노선 정보를 조회합니다.
     * 
     * @param trainScheduleId 열차 스케줄 ID
     * @return 노선 정보 (예: "서울-부산")
     */
    String getRouteInfo(Long trainScheduleId);
    
    /**
     * 열차의 실제 도착 시간을 조회합니다.
     * 
     * @param trainScheduleId 열차 스케줄 ID
     * @return 실제 도착 시간
     */
    LocalDateTime getActualArrivalTime(Long trainScheduleId);
    
    /**
     * 열차의 예정 도착 시간을 조회합니다.
     * 
     * @param trainScheduleId 열차 스케줄 ID
     * @return 예정 도착 시간
     */
    LocalDateTime getScheduledArrivalTime(Long trainScheduleId);
    
    /**
     * 열차 지연 시간을 분 단위로 조회합니다.
     * 
     * @param trainScheduleId 열차 스케줄 ID
     * @return 지연 시간 (분)
     */
    int getDelayMinutes(Long trainScheduleId);
}