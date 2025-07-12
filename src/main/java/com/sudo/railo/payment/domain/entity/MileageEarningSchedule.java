package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 적립 스케줄 엔티티
 * TrainSchedule과 Payment를 연결하여 실시간 마일리지 적립을 관리
 */
@Entity
@Table(
    name = "mileage_earning_schedules",
    indexes = {
        @Index(name = "idx_mileage_schedule_train", columnList = "train_schedule_id, status"),
        @Index(name = "idx_mileage_schedule_payment", columnList = "payment_id"),
        @Index(name = "idx_mileage_schedule_member", columnList = "member_id, status"),
        @Index(name = "idx_mileage_schedule_processing", columnList = "status, scheduled_earning_time")
    }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageEarningSchedule extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    
    @Column(name = "train_schedule_id", nullable = false)
    private Long trainScheduleId;       // TrainSchedule ID
    
    @Column(name = "payment_id", nullable = false, length = 255)
    private String paymentId;           // Payment ID (external_order_id)
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;              // 회원 ID
    
    @Column(name = "original_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal originalAmount;  // 원본 결제 금액
    
    @Column(name = "base_mileage_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal baseMileageAmount; // 기본 마일리지 적립액 (1%)
    
    @Column(name = "delay_compensation_rate", precision = 5, scale = 3)
    private BigDecimal delayCompensationRate; // 지연 보상 비율 (0.125, 0.25, 0.5)
    
    @Column(name = "delay_compensation_amount", precision = 12, scale = 0, columnDefinition = "DECIMAL(12,0) DEFAULT 0")
    @Builder.Default
    private BigDecimal delayCompensationAmount = BigDecimal.ZERO; // 지연 보상 마일리지
    
    @Column(name = "total_mileage_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal totalMileageAmount; // 총 마일리지 적립액
    
    @Column(name = "scheduled_earning_time", nullable = false)
    private LocalDateTime scheduledEarningTime; // 적립 예정 시간 (실제 도착 시간)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EarningStatus status;       // 적립 상태
    
    @Column(name = "delay_minutes", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private int delayMinutes = 0;       // 지연 시간(분)
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // 처리 완료 시간
    
    private Long baseTransactionId;     // 기본 마일리지 거래 ID
    
    private Long compensationTransactionId; // 지연 보상 거래 ID
    
    @Column(name = "error_message", length = 500)
    private String errorMessage;        // 에러 메시지
    
    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private int retryCount = 0;         // 재시도 횟수
    
    @Column(name = "route_info", length = 100)
    private String routeInfo;           // 노선 정보 (예: "서울-부산")
    
    /**
     * 마일리지 적립 상태
     */
    public enum EarningStatus {
        SCHEDULED("적립 예정"),         // 스케줄 생성됨
        READY("적립 준비"),            // 도착 시간 도달
        BASE_PROCESSING("기본 적립 중"), // 기본 마일리지 적립 중
        BASE_COMPLETED("기본 적립 완료"), // 기본 마일리지 적립 완료
        COMPENSATION_PROCESSING("보상 적립 중"), // 지연 보상 적립 중
        FULLY_COMPLETED("완전 완료"),   // 모든 적립 완료
        FAILED("적립 실패"),           // 적립 실패
        CANCELLED("적립 취소");        // 적립 취소 (환불 등)
        
        private final String description;
        
        EarningStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 적립 준비 상태로 변경
     */
    public void markReady() {
        this.status = EarningStatus.READY;
    }
    
    /**
     * 기본 마일리지 적립 시작
     */
    public void startBaseProcessing() {
        this.status = EarningStatus.BASE_PROCESSING;
    }
    
    /**
     * 기본 마일리지 적립 완료
     */
    public void completeBaseEarning(Long transactionId) {
        this.baseTransactionId = transactionId;
        
        if (hasDelayCompensation()) {
            this.status = EarningStatus.BASE_COMPLETED;
        } else {
            this.status = EarningStatus.FULLY_COMPLETED;
            this.processedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 지연 보상 적립 시작
     */
    public void startCompensationProcessing() {
        this.status = EarningStatus.COMPENSATION_PROCESSING;
    }
    
    /**
     * 지연 보상 적립 완료
     */
    public void completeCompensationEarning(Long transactionId) {
        this.compensationTransactionId = transactionId;
        this.status = EarningStatus.FULLY_COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 적립 실패 처리
     */
    public void fail(String errorMessage) {
        this.status = EarningStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 지연 보상 여부 확인
     */
    public boolean hasDelayCompensation() {
        return delayCompensationAmount != null && 
               delayCompensationAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 처리 완료 여부 확인
     */
    public boolean isCompleted() {
        return status == EarningStatus.FULLY_COMPLETED;
    }
    
    /**
     * 처리 가능 여부 확인
     */
    public boolean isReadyForProcessing(LocalDateTime currentTime) {
        return status == EarningStatus.READY && 
               currentTime.isAfter(scheduledEarningTime);
    }
    
    /**
     * 지연 정보 업데이트
     */
    public void updateDelayInfo(int delayMinutes, BigDecimal compensationRate) {
        this.delayMinutes = delayMinutes;
        this.delayCompensationRate = compensationRate;
        
        if (compensationRate.compareTo(BigDecimal.ZERO) > 0) {
            this.delayCompensationAmount = originalAmount
                .multiply(compensationRate)
                .setScale(0, BigDecimal.ROUND_DOWN);
            this.totalMileageAmount = baseMileageAmount.add(delayCompensationAmount);
        }
    }
    
    /**
     * 적립 예정 시간 업데이트
     */
    public void updateScheduledEarningTime(LocalDateTime newTime) {
        if (newTime == null) {
            throw new IllegalArgumentException("적립 예정 시간은 null일 수 없습니다");
        }
        this.scheduledEarningTime = newTime;
    }
    
    /**
     * 정상 운행 마일리지 스케줄 생성 팩토리 메서드
     */
    public static MileageEarningSchedule createNormalEarningSchedule(
            Long trainScheduleId,
            String paymentId,
            Long memberId,
            BigDecimal originalAmount,
            LocalDateTime scheduledEarningTime,
            String routeInfo) {
        
        BigDecimal baseMileageAmount = originalAmount
            .multiply(new BigDecimal("0.01"))
            .setScale(0, BigDecimal.ROUND_DOWN);
        
        return MileageEarningSchedule.builder()
                .trainScheduleId(trainScheduleId)
                .paymentId(paymentId)
                .memberId(memberId)
                .originalAmount(originalAmount)
                .baseMileageAmount(baseMileageAmount)
                .delayCompensationRate(BigDecimal.ZERO)
                .delayCompensationAmount(BigDecimal.ZERO)
                .totalMileageAmount(baseMileageAmount)
                .scheduledEarningTime(scheduledEarningTime)
                .status(EarningStatus.SCHEDULED)
                .delayMinutes(0)
            .retryCount(0)
            .routeInfo(routeInfo)
                .build();
    }
    
    /**
     * 지연 운행 마일리지 스케줄 생성 팩토리 메서드
     */
    public static MileageEarningSchedule createDelayedEarningSchedule(
            Long trainScheduleId,
            String paymentId,
            Long memberId,
            BigDecimal originalAmount,
            LocalDateTime scheduledEarningTime,
            int delayMinutes,
            BigDecimal compensationRate,
            String routeInfo) {
        
        BigDecimal baseMileageAmount = originalAmount
            .multiply(new BigDecimal("0.01"))
            .setScale(0, BigDecimal.ROUND_DOWN);
        
        BigDecimal compensationAmount = originalAmount
            .multiply(compensationRate)
            .setScale(0, BigDecimal.ROUND_DOWN);
        
        BigDecimal totalAmount = baseMileageAmount.add(compensationAmount);
        
        return MileageEarningSchedule.builder()
                .trainScheduleId(trainScheduleId)
                .paymentId(paymentId)
                .memberId(memberId)
                .originalAmount(originalAmount)
                .baseMileageAmount(baseMileageAmount)
                .delayCompensationRate(compensationRate)
                .delayCompensationAmount(compensationAmount)
            .totalMileageAmount(totalAmount)
                .scheduledEarningTime(scheduledEarningTime)
                .status(EarningStatus.SCHEDULED)
                .delayMinutes(delayMinutes)
            .retryCount(0)
            .routeInfo(routeInfo)
                .build();
    }
    
    /**
     * 환불 상태로 업데이트
     */
    public void updateRefundStatus(EarningStatus status) {
        this.status = status;
    }
} 