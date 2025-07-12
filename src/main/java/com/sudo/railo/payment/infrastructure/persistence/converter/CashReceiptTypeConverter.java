package com.sudo.railo.payment.infrastructure.persistence.converter;

import com.sudo.railo.payment.domain.entity.CashReceipt;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CashReceiptType enum과 DB 값을 변환하는 JPA 컨버터
 * 
 * DB에 저장된 "personal", "business" 값을 enum으로 매핑
 */
@Converter(autoApply = false)
public class CashReceiptTypeConverter implements AttributeConverter<CashReceipt.CashReceiptType, String> {
    
    @Override
    public String convertToDatabaseColumn(CashReceipt.CashReceiptType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }
    
    @Override
    public CashReceipt.CashReceiptType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        // fromCode 메서드를 사용하여 code 값으로 enum 찾기
        return CashReceipt.CashReceiptType.fromCode(dbData);
    }
}