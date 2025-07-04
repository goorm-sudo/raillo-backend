package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SendCodeRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;
import com.sudo.railo.member.application.dto.response.VerifyCodeResponse;

public interface MemberAuthService {

	SignUpResponse signUp(SignUpRequest request);

	TokenResponse memberNoLogin(MemberNoLoginRequest request);

	void logout(String accessToken);

	ReissueTokenResponse reissueAccessToken(String refreshToken);

	void sendAuthCode(SendCodeRequest request);

	VerifyCodeResponse verifyAuthCode(VerifyCodeRequest request);

}
