package com.sudo.railo.member.application;

import com.sudo.railo.member.application.dto.request.SignUpRequest;
import com.sudo.railo.member.application.dto.response.SignUpResponse;

public interface MemberAuthService {

	SignUpResponse signUp(SignUpRequest request);
}
