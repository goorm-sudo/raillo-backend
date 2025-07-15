package com.sudo.railo.payment.infrastructure.adapter.out.train;

import com.sudo.railo.payment.application.port.out.LoadTrainSchedulePort;
import com.sudo.railo.train.application.TrainScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 열차 스케줄 정보 어댑터
 * 
 * 헥사고날 아키텍처의 어댑터로, Train 도메인과의
 * 통신을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class TrainScheduleAdapter implements LoadTrainSchedulePort {
    
    private final TrainScheduleService trainScheduleService;
    
    @Override
    public String getRouteInfo(Long trainScheduleId) {
        return trainScheduleService.getRouteInfo(trainScheduleId);
    }
    
    @Override
    public LocalDateTime getActualArrivalTime(Long trainScheduleId) {
        // TrainScheduleService에서 실제 도착 시간을 조회하는 메서드가 필요
        // 현재는 예정 도착 시간을 반환하도록 임시 구현
        // TODO: TrainScheduleService에 getActualArrivalTime 메서드 추가 필요
        return getScheduledArrivalTime(trainScheduleId);
    }
    
    @Override
    public LocalDateTime getScheduledArrivalTime(Long trainScheduleId) {
        // TrainScheduleService에서 예정 도착 시간을 조회하는 메서드가 필요
        // TODO: TrainScheduleService에 getScheduledArrivalTime 메서드 추가 필요
        return LocalDateTime.now().plusHours(3); // 임시 구현
    }
    
    @Override
    public int getDelayMinutes(Long trainScheduleId) {
        // TrainScheduleService에서 지연 시간을 조회하는 메서드가 필요
        // TODO: TrainScheduleService에 getDelayMinutes 메서드 추가 필요
        return 0; // 임시 구현
    }
}