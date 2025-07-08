package com.sudo.railo.member.application;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.exception.AuthError;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

	private final JavaMailSender mailSender;

	private static final String TITLE = "Raillo Email Verification Code";
	private static final String HTML_TEMPLATE = """
		<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
		  <h2>Raillo 이메일 인증</h2>
		  <p>안녕하세요! 아래의 인증 코드를 입력해주세요.</p>
		  <div style="background-color: #f5f5f5; padding: 20px; text-align: center; font-size: 24px; font-weight: bold;">
		    %s
		  </div>
		  <p>이 코드는 5분간 유효합니다.</p>
		</div>
		""";

	public void sendEmail(String email, String code) {
		String html = String.format(HTML_TEMPLATE, code);
		try {
			MimeMessage emailForm = createEmailMessage(email, html);
			mailSender.send(emailForm);
		} catch (RuntimeException | MessagingException e) {
			log.error(e.getMessage());
			throw new BusinessException(AuthError.SEND_EMAIL_FAIL);
		}
	}

	private MimeMessage createEmailMessage(String email, String htmlContent) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		helper.setTo(email);
		helper.setSubject(TITLE);
		helper.setText(htmlContent, true); // html 사용
		return message;
	}
}
