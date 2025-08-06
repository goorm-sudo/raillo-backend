package com.sudo.railo.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieManager {

	private static final String COOKIE_PATH = "/";

	@Value("${cookie.domain}")
	private String cookieDomain;

	public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
		String cookieHeader = String.format(
			"%s=%s; Max-Age=%d; Path=%s; Domain=%s; Secure; HttpOnly; SameSite=None",
			name, value, maxAge, COOKIE_PATH, cookieDomain
		);
		response.setHeader("Set-Cookie", cookieHeader); // 중복 방지: addHeader → setHeader
	}

	public void removeCookie(HttpServletResponse response, String name) {
		String cookieHeader = String.format(
			"%s=; Max-Age=0; Path=%s; Domain=%s; Secure; HttpOnly; SameSite=None",
			name, COOKIE_PATH, cookieDomain
		);
		response.setHeader("Set-Cookie", cookieHeader); // 중복 방지
	}
}
