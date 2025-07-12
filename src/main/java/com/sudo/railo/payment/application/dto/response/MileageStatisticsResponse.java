package com.sudo.railo.payment.application.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 마일리지 통계 응답 DTO
 * 회원의 마일리지 사용 통계 정보를 담은 응답 클래스
 */
@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class MileageStatisticsResponse {
    
    private Long memberId;
    private MileageStatistics statistics;
    private LocalDateTime calculatedAt;
    
    /**
     * 마일리지 통계 정보로부터 응답 생성
     */
    public static MileageStatisticsResponse from(Long memberId, MileageStatistics statistics) {
        return MileageStatisticsResponse.builder()
                .memberId(memberId)
                .statistics(statistics)
                .calculatedAt(LocalDateTime.now())
                .build();
    }
} 