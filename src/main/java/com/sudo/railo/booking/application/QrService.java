package com.sudo.railo.booking.application;

import org.springframework.stereotype.Service;

import com.sudo.railo.booking.domain.Qr;
import com.sudo.railo.booking.exception.BookingError;
import com.sudo.railo.booking.infra.QrRepository;
import com.sudo.railo.global.exception.error.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QrService {

	private final QrRepository qrRepository;

	public Qr createQr() {
		Qr qr = Qr.builder()
			.isUsable(false)
			.scanCount(0)
			.build();
		try {
			return qrRepository.save(qr);
		} catch (Exception e) {
			throw new BusinessException(BookingError.QR_CREATE_FAILED);
		}
	}
}
