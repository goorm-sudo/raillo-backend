package com.sudo.railo.member.application;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.member.application.dto.response.SendCodeResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

	private final EmailSendService emailSendService;
	private final AuthRedisRepository authRedisRepository;

	/**
	 * 이메일 인증 코드 전송
	 * */
	public SendCodeResponse sendAuthCode(String email) {
		String code = createAuthCode();
		emailSendService.sendEmail(email, code);
		authRedisRepository.saveAuthCode(email, code);
		return new SendCodeResponse(email);
	}

	/**
	 * 이메일 인증 코드 검증
	 * */
	public boolean verifyAuthCode(String email, String authCode) {
		String findCode = authRedisRepository.getAuthCode(email);
		boolean isVerified = authCode.equals(findCode);
		
		if (isVerified) {
			authRedisRepository.deleteAuthCode(email);
		}

		return isVerified;
	}

	/**
	 * 랜덤 인증 코드 생성
	 * */
	private String createAuthCode() {
		SecureRandom random = new SecureRandom();
		return String.format("%06d", random.nextInt(1000000));
	}
}
