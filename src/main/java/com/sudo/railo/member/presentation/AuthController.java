package com.sudo.railo.member.presentation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.security.jwt.TokenExtractor;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.AuthService;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;
import com.sudo.railo.member.docs.AuthControllerDocs;
import com.sudo.railo.member.success.AuthSuccess;

import jakarta.servlet.http.HttpServletRequest;
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
	public SuccessResponse<TokenResponse> memberNoLogin(@RequestBody @Valid MemberNoLoginRequest request) {

		TokenResponse tokenResponse = authService.memberNoLogin(request);

		return SuccessResponse.of(AuthSuccess.MEMBER_NO_LOGIN_SUCCESS, tokenResponse);
	}

	@PostMapping("/logout")
	public SuccessResponse<?> logout(HttpServletRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String accessToken = tokenExtractor.resolveToken(request);

		authService.logout(accessToken, memberNo);

		return SuccessResponse.of(AuthSuccess.LOGOUT_SUCCESS);
	}

	@PostMapping("/reissue")
	public SuccessResponse<ReissueTokenResponse> reissue(HttpServletRequest request,
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		String refreshToken = tokenExtractor.resolveToken(request);

		ReissueTokenResponse tokenResponse = authService.reissueAccessToken(refreshToken, memberNo);

		return SuccessResponse.of(AuthSuccess.REISSUE_TOKEN_SUCCESS, tokenResponse);
	}

}
