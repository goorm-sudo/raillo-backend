package com.sudo.railo.payment.domain.entity;

import com.sudo.railo.global.domain.BaseEntity;
import com.sudo.railo.payment.domain.constant.PaymentPrecision;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 거래 내역 엔티티
 * 적립, 사용, 만료, 조정 등 모든 마일리지 변동 내역을 기록
 */
@Entity
@Table(
    name = "mileage_transactions",
    indexes = {
        @Index(name = "idx_mileage_tx_member", columnList = "member_id, transaction_type, status"),
        @Index(name = "idx_mileage_tx_payment", columnList = "payment_id"),
        @Index(name = "idx_mileage_tx_schedule", columnList = "train_schedule_id"),
        @Index(name = "idx_mileage_tx_earning_schedule", columnList = "earning_schedule_id")
    }
)
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageTransaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    
    private String paymentId;           // 결제와 연결 (적립/사용 시)
    
    // 🆕 새로운 마일리지 시스템용 필드들
    private Long trainScheduleId;       // TrainSchedule과 연결
    
    private Long earningScheduleId;     // MileageEarningSchedule과 연결
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;       // EARN, USE, EXPIRE, ADJUST
    
    // 🆕 확장된 거래 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "earning_type")
    private EarningType earningType;    // 적립 타입 (기본, 지연보상)
    
    @Column(name = "points_amount", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE, nullable = false)
    private BigDecimal pointsAmount;    // 포인트 수량 (양수: 적립, 음수: 차감)
    
    @Column(name = "balance_before", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE, nullable = false)
    private BigDecimal balanceBefore;   // 거래 전 잔액
    
    @Column(name = "balance_after", precision = PaymentPrecision.MILEAGE_PRECISION, scale = PaymentPrecision.MILEAGE_SCALE, nullable = false)
    private BigDecimal balanceAfter;    // 거래 후 잔액
    
    @Column(name = "description", length = 500)
    private String description;         // 거래 설명
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;    // 적립 포인트 만료일 (적립 시에만)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;   // PENDING, COMPLETED, CANCELLED
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;  // 처리 완료 시간
    
    // 🆕 지연 정보 필드들
    @Column(name = "delay_minutes", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private int delayMinutes = 0;       // 지연 시간(분)
    
    @Column(name = "compensation_rate", precision = PaymentPrecision.RATE_PRECISION, scale = PaymentPrecision.RATE_SCALE)
    private BigDecimal compensationRate; // 지연 보상 비율
    
    @Version
    @Column(name = "version")
    private Long version;
    
    /**
     * 마일리지 거래 유형
     */
    public enum TransactionType {
        EARN("적립"),           // 구매 시 적립
        USE("사용"),            // 결제 시 사용
        EXPIRE("만료"),         // 유효기간 만료
        ADJUST("조정"),         // 관리자 조정
        REFUND("환불");         // 환불로 인한 복구
        
        private final String description;
        
        TransactionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 🆕 적립 타입 (새로운 마일리지 시스템용)
     */
    public enum EarningType {
        BASE_EARN("기본 적립"),           // 일반 1% 적립
        DELAY_COMPENSATION("지연 보상"),   // 지연으로 인한 보상 적립
        PROMOTION("프로모션"),           // 프로모션 적립
        MANUAL_ADJUST("수동 조정");       // 관리자 수동 조정
        
        private final String description;
        
        EarningType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 거래 상태
     */
    public enum TransactionStatus {
        PENDING("처리 대기"),       // 거래 대기 중
        COMPLETED("완료"),         // 거래 완료
        CANCELLED("취소"),         // 거래 취소
        FAILED("실패");           // 거래 실패
        
        private final String description;
        
        TransactionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 거래 완료 처리
     */
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 거래 취소 처리
     */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
        this.processedAt = LocalDateTime.now();
    }
    
    /**
     * 적립 거래 생성 팩토리 메서드
     */
    public static MileageTransaction createEarnTransaction(
            Long memberId, 
            String paymentId, 
            BigDecimal pointsAmount, 
            BigDecimal balanceBefore,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(TransactionType.EARN)
                .earningType(EarningType.BASE_EARN)
                .pointsAmount(pointsAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.add(pointsAmount))
                .description(description)
                .expiresAt(LocalDateTime.now().plusYears(5)) // 🆕 5년 후 만료
                .status(TransactionStatus.PENDING)
                .build();
    }
    
    /**
     * 🆕 기본 마일리지 적립 거래 생성 (새로운 시스템용)
     */
    public static MileageTransaction createBaseEarningTransaction(
            Long memberId,
            String paymentId,
            Long trainScheduleId,
            Long earningScheduleId,
            BigDecimal pointsAmount,
            BigDecimal balanceBefore,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .trainScheduleId(trainScheduleId)
                .earningScheduleId(earningScheduleId)
                .type(TransactionType.EARN)
                .earningType(EarningType.BASE_EARN)
                .pointsAmount(pointsAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.add(pointsAmount))
                .description(description)
                .expiresAt(LocalDateTime.now().plusYears(5)) // 5년 후 만료
                .status(TransactionStatus.PENDING)
                .delayMinutes(0)
                .build();
    }
    
    /**
     * 🆕 지연 보상 마일리지 적립 거래 생성
     */
    public static MileageTransaction createDelayCompensationTransaction(
            Long memberId,
            String paymentId,
            Long trainScheduleId,
            Long earningScheduleId,
            BigDecimal pointsAmount,
            BigDecimal balanceBefore,
            int delayMinutes,
            BigDecimal compensationRate,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .trainScheduleId(trainScheduleId)
                .earningScheduleId(earningScheduleId)
                .type(TransactionType.EARN)
                .earningType(EarningType.DELAY_COMPENSATION)
                .pointsAmount(pointsAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.add(pointsAmount))
                .description(description)
                .expiresAt(LocalDateTime.now().plusYears(5)) // 5년 후 만료
                .status(TransactionStatus.PENDING)
                .delayMinutes(delayMinutes)
                .compensationRate(compensationRate)
                .build();
    }
    
    /**
     * 사용 거래 생성 팩토리 메서드
     */
    public static MileageTransaction createUseTransaction(
            Long memberId, 
            String paymentId, 
            BigDecimal pointsAmount, 
            BigDecimal balanceBefore,
            String description) {
        
        return MileageTransaction.builder()
                .memberId(memberId)
                .paymentId(paymentId)
                .type(TransactionType.USE)
                .pointsAmount(pointsAmount.negate()) // 음수로 저장
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceBefore.subtract(pointsAmount))
                .description(description)
                .status(TransactionStatus.PENDING)
                .build();
    }
    
    /**
     * 🆕 지연 보상 여부 확인
     */
    public boolean isDelayCompensation() {
        return earningType == EarningType.DELAY_COMPENSATION;
    }
    
    /**
     * 🆕 기본 적립 여부 확인
     */
    public boolean isBaseEarning() {
        return earningType == EarningType.BASE_EARN;
    }
} 