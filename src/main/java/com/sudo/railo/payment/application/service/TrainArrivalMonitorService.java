package com.sudo.railo.payment.application.service;

import com.sudo.railo.payment.application.service.DomainEventOutboxService;
import com.sudo.railo.payment.application.port.in.UpdateMileageEarningScheduleUseCase;
import com.sudo.railo.train.domain.TrainSchedule;
import com.sudo.railo.train.infrastructure.TrainScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 열차 도착 모니터링 서비스
 * TrainSchedule을 모니터링하여 도착한 열차를 체크하고 
 * 마일리지 적립 스케줄을 준비 상태로 변경
 * 
 * 향후 확장 계획:
 * - KTX 관제시스템 API 연동
 * - 실시간 열차 위치 및 도착 정보 수신
 * - 자동 스케줄러를 통한 실시간 마일리지 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainArrivalMonitorService {
    
    private final TrainScheduleRepository trainScheduleRepository;
    private final UpdateMileageEarningScheduleUseCase updateMileageEarningScheduleUseCase;
    private final DomainEventOutboxService domainEventOutboxService;
    
    /**
     * 도착한 열차들을 체크하고 처리
     * @return 처리된 열차 수
     */
    @Transactional
    public int checkAndProcessArrivedTrains() {
        log.debug("도착한 열차 체크 및 처리 시작");
        
        LocalDateTime currentTime = LocalDateTime.now();
        
        // 마일리지 처리가 필요한 도착한 열차 조회
        List<TrainSchedule> arrivedTrains = trainScheduleRepository
            .findArrivedTrainsForMileageProcessing(currentTime);
        
        // 배치 크기 제한 (50개씩 처리)
        if (arrivedTrains.size() > 50) {
            arrivedTrains = arrivedTrains.subList(0, 50);
        }
        
        int processedCount = 0;
        
        for (TrainSchedule train : arrivedTrains) {
            try {
                processArrivedTrain(train);
                processedCount++;
            } catch (Exception e) {
                log.error("열차 도착 처리 실패 - TrainScheduleId: {}", train.getId(), e);
            }
        }
        
        log.debug("도착한 열차 체크 및 처리 완료 - 처리된 열차 수: {}", processedCount);
        
        return processedCount;
    }
    
    /**
     * 개별 도착한 열차 처리
     */
    @Transactional
    public void processArrivedTrain(TrainSchedule trainSchedule) {
        log.info("열차 도착 처리 시작 - TrainScheduleId: {}, 도착시간: {}", 
                trainSchedule.getId(), trainSchedule.getActualArrivalTime());
        
        try {
            // 1. 열차 도착 이벤트 발행
            domainEventOutboxService.publishTrainArrivedEvent(
                    trainSchedule.getId(), 
                    trainSchedule.getActualArrivalTime()
            );
            
            // 2. 지연이 있는 경우 지연 이벤트도 발행
            if (trainSchedule.hasSignificantDelay()) {
                domainEventOutboxService.publishTrainDelayedEvent(
                        trainSchedule.getId(),
                        trainSchedule.getDelayMinutes(),
                        trainSchedule.getActualArrivalTime()
                );
                
                // 지연 보상 마일리지 스케줄 업데이트
                UpdateMileageEarningScheduleUseCase.UpdateDelayCompensationCommand delayCommand =
                    new UpdateMileageEarningScheduleUseCase.UpdateDelayCompensationCommand(
                        trainSchedule.getId(),
                        trainSchedule.getDelayMinutes(),
                        trainSchedule.getActualArrivalTime()
                    );
                updateMileageEarningScheduleUseCase.updateDelayCompensation(delayCommand);
            }
            
            // 3. 마일리지 적립 스케줄을 READY 상태로 변경
            UpdateMileageEarningScheduleUseCase.MarkScheduleReadyCommand readyCommand =
                new UpdateMileageEarningScheduleUseCase.MarkScheduleReadyCommand(
                    trainSchedule.getId(), 
                    trainSchedule.getActualArrivalTime()
                );
            updateMileageEarningScheduleUseCase.markScheduleReady(readyCommand);
            
            // 4. TrainSchedule의 마일리지 처리 완료 표시
            trainSchedule.markMileageProcessed();
            trainScheduleRepository.save(trainSchedule);
            
            log.info("열차 도착 처리 완료 - TrainScheduleId: {}", trainSchedule.getId());
            
        } catch (Exception e) {
            log.error("열차 도착 처리 중 오류 발생 - TrainScheduleId: {}", trainSchedule.getId(), e);
            throw e;
        }
    }
    
    /**
     * 임시 메서드: Train 도메인 협업 전까지 사용
     */
    private void processArrivedTrainMock(Long trainScheduleId, LocalDateTime actualArrivalTime, int delayMinutes) {
        log.info("열차 도착 처리 시작 (Mock) - TrainScheduleId: {}, 도착시간: {}", 
                trainScheduleId, actualArrivalTime);
        
        try {
            // 1. 열차 도착 이벤트 발행
            domainEventOutboxService.publishTrainArrivedEvent(trainScheduleId, actualArrivalTime);
            
            // 2. 지연이 있는 경우 지연 이벤트도 발행
            if (delayMinutes >= 20) {
                domainEventOutboxService.publishTrainDelayedEvent(
                        trainScheduleId, delayMinutes, actualArrivalTime);
                
                // 지연 보상 마일리지 스케줄 업데이트
                UpdateMileageEarningScheduleUseCase.UpdateDelayCompensationCommand delayCommand =
                    new UpdateMileageEarningScheduleUseCase.UpdateDelayCompensationCommand(
                        trainScheduleId, delayMinutes, actualArrivalTime);
                updateMileageEarningScheduleUseCase.updateDelayCompensation(delayCommand);
            }
            
            // 3. 마일리지 적립 스케줄을 READY 상태로 변경
            UpdateMileageEarningScheduleUseCase.MarkScheduleReadyCommand readyCommand =
                new UpdateMileageEarningScheduleUseCase.MarkScheduleReadyCommand(
                    trainScheduleId, actualArrivalTime);
            updateMileageEarningScheduleUseCase.markScheduleReady(readyCommand);
            
            log.info("열차 도착 처리 완료 (Mock) - TrainScheduleId: {}", trainScheduleId);
            
        } catch (Exception e) {
            log.error("열차 도착 처리 중 오류 발생 (Mock) - TrainScheduleId: {}", trainScheduleId, e);
            throw e;
        }
    }
    
    /**
     * 테스트용 메서드: 임의의 열차 도착 시뮬레이션
     */
    @Transactional
    public void simulateTrainArrival(Long trainScheduleId, LocalDateTime actualArrivalTime, int delayMinutes) {
        log.info("열차 도착 시뮬레이션 - TrainScheduleId: {}, 지연: {}분", trainScheduleId, delayMinutes);
        
        processArrivedTrainMock(trainScheduleId, actualArrivalTime, delayMinutes);
    }
} 