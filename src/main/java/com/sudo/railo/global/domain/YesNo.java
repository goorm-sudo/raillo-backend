package com.sudo.railo.global.domain;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.exception.error.GlobalError;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum YesNo {

	Y("Y", true),
	N("N", false);

	private final String value;
	private final boolean booleanValue;

	public static YesNo from(boolean value) {
		return value ? Y : N;
	}

	public static YesNo from(String value) {
		if ("Y".equalsIgnoreCase(value)) {
			return Y;
		} else if ("N".equalsIgnoreCase(value)) {
			return N;
		} else {
			throw new BusinessException(GlobalError.INVALID_YN_VALUE);
		}
	}
}
