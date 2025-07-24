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
import com.sudo.railo.auth.util.CookieManager;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.success.SuccessResponse;

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
	private final CookieManager cookieManager;

	private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

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

		cookieManager.setCookie(response, REFRESH_TOKEN_COOKIE_NAME, tokenResponse.refreshToken(),
			REFRESH_TOKEN_MAX_AGE);

		return SuccessResponse.of(AuthSuccess.LOGIN_SUCCESS, loginResponse);
	}

	@PostMapping("/logout")
	public SuccessResponse<?> logout(HttpServletRequest request, HttpServletResponse response,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String accessToken = tokenExtractor.resolveToken(request);

		authService.logout(accessToken, memberNo);

		cookieManager.removeCookie(response, REFRESH_TOKEN_COOKIE_NAME);

		return SuccessResponse.of(AuthSuccess.LOGOUT_SUCCESS);
	}

	@PostMapping("/reissue")
	public SuccessResponse<ReissueTokenResponse> reissue(
		@CookieValue(value = "refreshToken", required = false) String refreshToken) {

		if (refreshToken == null || refreshToken.isEmpty()) {
			throw new BusinessException(TokenError.INVALID_REFRESH_TOKEN);
		}

		ReissueTokenResponse tokenResponse = authService.reissueAccessToken(refreshToken);

		return SuccessResponse.of(AuthSuccess.REISSUE_TOKEN_SUCCESS, tokenResponse);
	}
	
}
