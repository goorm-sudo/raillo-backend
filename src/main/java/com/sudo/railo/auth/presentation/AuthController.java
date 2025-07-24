package com.sudo.railo.auth.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.auth.application.AuthService;
import com.sudo.railo.auth.application.dto.request.LoginRequest;
import com.sudo.railo.auth.application.dto.request.SignUpRequest;
import com.sudo.railo.auth.application.dto.response.LoginResponse;
import com.sudo.railo.auth.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.auth.application.dto.response.SignUpResponse;
import com.sudo.railo.auth.application.dto.response.TokenResponse;
import com.sudo.railo.auth.docs.AuthControllerDocs;
import com.sudo.railo.auth.exception.TokenError;
import com.sudo.railo.auth.security.jwt.TokenExtractor;
import com.sudo.railo.auth.success.AuthSuccess;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.success.SuccessResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

	private final AuthService authService;
	private final TokenExtractor tokenExtractor;

	private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private static final String COOKIE_PATH = "/";

	@PostMapping("/signup")
	public SuccessResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {

		SignUpResponse response = authService.signUp(request);

		return SuccessResponse.of(AuthSuccess.SIGN_UP_SUCCESS, response);
	}

	@PostMapping("/login")
	public SuccessResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
		HttpServletResponse response) {

		TokenResponse tokenResponse = authService.login(request);
		LoginResponse loginResponse = new LoginResponse(tokenResponse.grantType(), tokenResponse.accessToken(),
			tokenResponse.accessTokenExpiresIn());

		setRefreshTokenCookie(response, tokenResponse.refreshToken());

		return SuccessResponse.of(AuthSuccess.LOGIN_SUCCESS, loginResponse);
	}

	@PostMapping("/logout")
	public SuccessResponse<?> logout(HttpServletRequest request, HttpServletResponse response,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String accessToken = tokenExtractor.resolveToken(request);

		authService.logout(accessToken, memberNo);

		removeRefreshTokenCookie(response);

		return SuccessResponse.of(AuthSuccess.LOGOUT_SUCCESS);
	}

	@PostMapping("/reissue")
	public SuccessResponse<ReissueTokenResponse> reissue(@CookieValue("refreshToken") String refreshToken) {

		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new BusinessException(TokenError.INVALID_REFRESH_TOKEN);
		}

		ReissueTokenResponse tokenResponse = authService.reissueAccessToken(refreshToken);

		return SuccessResponse.of(AuthSuccess.REISSUE_TOKEN_SUCCESS, tokenResponse);
	}

	private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
		cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
		cookie.setSecure(true); // HTTPS 환경에서만 전송
		cookie.setHttpOnly(true); // JavaScript 접근 차단
		cookie.setPath(COOKIE_PATH); // 모든 경로에서 쿠키 전송 가능

		response.addCookie(cookie);
	}

	private void removeRefreshTokenCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
		cookie.setMaxAge(0);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setPath(COOKIE_PATH);

		response.addCookie(cookie);
	}
}
