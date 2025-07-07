package com.sudo.railo.member.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sudo.railo.global.security.jwt.TokenExtractor;
import com.sudo.railo.global.success.SuccessResponse;
import com.sudo.railo.member.application.MemberAuthService;
import com.sudo.railo.member.application.MemberService;
import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyCodeResponse;
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

	private final MemberAuthService memberAuthService;
	private final TokenExtractor tokenExtractor;
	private final MemberService memberService;

	@PostMapping("/signup")
	public SuccessResponse<SignUpResponse> signUp(@RequestBody @Valid SignUpRequest request) {

		SignUpResponse response = memberAuthService.signUp(request);

		return SuccessResponse.of(AuthSuccess.SIGN_UP_SUCCESS, response);
	}

	@PostMapping("/login")
	public SuccessResponse<TokenResponse> memberNoLogin(@RequestBody @Valid MemberNoLoginRequest request) {

		TokenResponse tokenResponse = memberAuthService.memberNoLogin(request);

		return SuccessResponse.of(AuthSuccess.MEMBER_NO_LOGIN_SUCCESS, tokenResponse);
	}

	@PostMapping("/logout")
	public SuccessResponse<?> logout(HttpServletRequest request) {

		String accessToken = tokenExtractor.resolveToken(request);

		memberAuthService.logout(accessToken);

		return SuccessResponse.of(AuthSuccess.LOGOUT_SUCCESS);
	}

	@PostMapping("/reissue")
	public SuccessResponse<ReissueTokenResponse> reissue(HttpServletRequest request) {

		String refreshToken = tokenExtractor.resolveToken(request);

		ReissueTokenResponse tokenResponse = memberAuthService.reissueAccessToken(refreshToken);

		return SuccessResponse.of(AuthSuccess.REISSUE_TOKEN_SUCCESS, tokenResponse);
	}

	/* 이메일 인증 (인증되지 않은 사용자) */
	@PostMapping("/emails")
	public SuccessResponse<SendCodeResponse> sendAuthCode(@RequestBody @Valid SendCodeRequest request) {

		String email = request.email();
		SendCodeResponse response = memberAuthService.sendAuthCode(email);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	/* 이메일 인증 (인증된 사용자) */
	@PostMapping("/members/emails")
	public SuccessResponse<?> sendAuthCodeWithMember(
		@AuthenticationPrincipal(expression = "username") String memberNo) {

		log.info("memberNo: {}", memberNo);

		String email = memberService.getMemberEmail(memberNo);
		SendCodeResponse response = memberAuthService.sendAuthCode(email);

		return SuccessResponse.of(AuthSuccess.SEND_CODE_SUCCESS, response);
	}

	/* 이메일 인증 (인증된 사용자, 인증되지 않은 사용자 모두 사용) */
	@PostMapping("/emails/verify")
	public ResponseEntity<VerifyCodeResponse> verifyAuthCode(@RequestBody @Valid VerifyCodeRequest request) {

		VerifyCodeResponse response = memberAuthService.verifyAuthCode(request);

		return ResponseEntity.ok(response);
	}

}
