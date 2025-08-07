package com.sudo.railo.payment.application.dto;

import java.math.BigDecimal;

import com.sudo.railo.payment.domain.status.PaymentStatus;
import com.sudo.railo.payment.domain.type.PaymentMethod;

public record PaymentInfo(BigDecimal amount, PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
}
