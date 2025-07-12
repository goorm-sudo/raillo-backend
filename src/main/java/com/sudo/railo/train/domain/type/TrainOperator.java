package com.sudo.railo.train.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 열차 운영사 구분
 */
@Getter
@RequiredArgsConstructor
public enum TrainOperator {
    KORAIL("한국철도공사", "코레일"),
    SRT("SR", "수서고속철도");
    
    private final String fullName;
    private final String shortName;
    
    /**
     * 열차 타입으로부터 운영사 확인
     */
    public static TrainOperator fromTrainType(TrainType trainType) {
        return switch (trainType) {
            case KTX, KTX_SANCHEON, KTX_CHEONGRYONG, KTX_EUM -> KORAIL;
            // 향후 SRT 타입 추가 시 매핑
            default -> KORAIL; // 기본값은 KORAIL
        };
    }
}