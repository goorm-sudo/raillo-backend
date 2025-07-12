package com.sudo.railo.payment.application.dto.response;

import com.sudo.railo.payment.domain.entity.MileageTransaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 마일리지 거래 내역 응답 DTO
 * 회원의 마일리지 거래 내역을 담은 응답 클래스
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageTransactionResponse {
    
    private List<MileageTransactionItem> transactions;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Boolean hasNext;
    private Boolean hasPrevious;
    
    /**
     * 마일리지 거래 내역 아이템
     */
    @Data
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MileageTransactionItem {
        
        private Long transactionId;
        private String transactionType;        // 거래 유형 (적립, 사용, 만료 등)
        private BigDecimal pointsAmount;       // 포인트 금액
        private BigDecimal balanceAfter;       // 거래 후 잔액
        private String description;            // 거래 설명
        private LocalDateTime processedAt;     // 처리 시간
        private LocalDateTime createdAt;       // 생성 시간
        private String status;                 // 상태 (COMPLETED, PENDING, CANCELLED)
        
        // 연관 정보
        private String relatedPaymentId;       // 연관 결제 ID
        private Long relatedTrainScheduleId;   // 연관 기차 스케줄 ID
        
        /**
         * MileageTransaction 엔티티로부터 응답 생성
         */
        public static MileageTransactionItem from(MileageTransaction transaction) {
            return MileageTransactionItem.builder()
                    .transactionId(transaction.getId())
                    .transactionType(transaction.getType().getDescription())
                    .pointsAmount(transaction.getPointsAmount())
                    .balanceAfter(transaction.getBalanceAfter())
                    .description(transaction.getDescription())
                    .processedAt(transaction.getProcessedAt())
                    .createdAt(transaction.getCreatedAt())
                    .status(transaction.getStatus().getDescription())
                    .relatedPaymentId(transaction.getPaymentId())
                    .relatedTrainScheduleId(transaction.getTrainScheduleId())
                    .build();
        }
    }
    
    /**
     * MileageTransaction 리스트로부터 응답 생성
     */
    public static MileageTransactionResponse from(List<MileageTransaction> transactions,
                                                 Long totalElements,
                                                 Integer totalPages,
                                                 Integer currentPage,
                                                 Integer pageSize,
                                                 Boolean hasNext,
                                                 Boolean hasPrevious) {
        List<MileageTransactionItem> items = transactions.stream()
                .map(MileageTransactionItem::from)
                .collect(Collectors.toList());
        
        return MileageTransactionResponse.builder()
                .transactions(items)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
} 