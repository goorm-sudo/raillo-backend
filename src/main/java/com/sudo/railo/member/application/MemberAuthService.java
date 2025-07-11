package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.response.ReissueTokenResponse;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;

public interface MemberAuthService {

	SignUpResponse signUp(SignUpRequest request);

	TokenResponse memberNoLogin(MemberNoLoginRequest request);

	void logout(String accessToken);

	ReissueTokenResponse reissueAccessToken(String refreshToken);

	SendCodeResponse sendAuthCode(String email);

	boolean verifyAuthCode(String email, String authCode);

}
