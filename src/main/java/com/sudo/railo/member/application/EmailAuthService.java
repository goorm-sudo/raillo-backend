package com.sudo.railo.member.application;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.AuthError;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

	private final JavaMailSender mailSender;

	private static final String TITLE = "Raillo Email Verification Code";
	private static final String TEXT_PREFIX = "Raillo 이메일 인증 코드입니다. 아래의 인증 코드를 입력해주세요.\nVerification Code : ";

	public void sendEmail(String email, String code) {
		String text = TEXT_PREFIX + code;
		SimpleMailMessage emailForm = createEmailMessage(email, text);
		try {
			mailSender.send(emailForm);
		} catch (RuntimeException e) {
			throw new BusinessException(AuthError.SEND_EMAIL_FAIL);
		}
	}

	private SimpleMailMessage createEmailMessage(String email, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject(TITLE);
		message.setText(text);
		return message;
	}
}
