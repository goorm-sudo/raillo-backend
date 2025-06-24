package com.sudo.railo.global.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.security.TokenError;

public class SecurityUtil {

	private static Authentication getAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new BusinessException(TokenError.INVALID_TOKEN);
		}
		return authentication;
	}

	public static String getCurrentMemberNo() {
		Authentication authentication = getAuthentication();
		return authentication.getName();
	}
}
