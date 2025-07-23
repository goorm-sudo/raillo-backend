package com.sudo.railo.auth.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

	private final AuthService authService;
	private final TokenExtractor tokenExtractor;

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

		Cookie cookie = new Cookie("refreshToken", tokenResponse.refreshToken());
		cookie.setMaxAge(7 * 24 * 60 * 60); // 7일
		cookie.setSecure(true); // secure cookie 적용
		cookie.setHttpOnly(true); // JavaScript 에서 접근 금지
		cookie.setPath("/"); // 모든 경로에서 쿠키 전송 가능

		response.addCookie(cookie);

		return SuccessResponse.of(AuthSuccess.MEMBER_NO_LOGIN_SUCCESS, loginResponse);
	}

	@PostMapping("/logout")
	public SuccessResponse<?> logout(HttpServletRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String accessToken = tokenExtractor.resolveToken(request);

		authService.logout(accessToken, memberNo);

		return SuccessResponse.of(AuthSuccess.LOGOUT_SUCCESS);
	}

	@PostMapping("/reissue")
	public SuccessResponse<ReissueTokenResponse> reissue(HttpServletRequest request) {

		// refreshToken 을 Cookie 에서 추출
		String refreshToken = null;
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookie.getName().equals("refreshToken")) {
					refreshToken = cookie.getValue();
					break;
				}
			}
		}

		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new BusinessException(TokenError.INVALID_REFRESH_TOKEN);
		}

		log.info("refreshToken: {}", refreshToken);

		ReissueTokenResponse tokenResponse = authService.reissueAccessToken(refreshToken);

		return SuccessResponse.of(AuthSuccess.REISSUE_TOKEN_SUCCESS, tokenResponse);
	}

}
