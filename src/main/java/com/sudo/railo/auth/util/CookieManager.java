package com.sudo.railo.auth.util;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieManager {

	private static final String COOKIE_PATH = "/";

	public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = createCookie(name, value, maxAge);
		response.addCookie(cookie);
	}

	public void removeCookie(HttpServletResponse response, String name) {
		Cookie cookie = createCookie(name, null, 0);
		response.addCookie(cookie);
	}

	private Cookie createCookie(String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(maxAge);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setPath(COOKIE_PATH);

		return cookie;
	}
}
