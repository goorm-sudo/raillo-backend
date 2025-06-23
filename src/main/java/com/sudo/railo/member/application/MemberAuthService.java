package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.MemberNoLoginRequest;
import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.request.TokenRequest;
import com.sudo.railo.member.application.dto.response.SignUpResponse;
import com.sudo.railo.member.application.dto.response.TokenResponse;

public interface MemberAuthService {

	SignUpResponse signUp(SignUpRequest request);

	TokenResponse memberNoLogin(MemberNoLoginRequest request);

	void logout(TokenRequest request);

}
