package com.sudo.railo.auth.application;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.support.annotation.ServiceTest;

@ServiceTest
class EmailAuthServiceTest {

	// GreenMail 확장 등록
	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(
		new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP) // 포트 충돌이 있어 동적 포트 할당
	)
		.withConfiguration(GreenMailConfiguration.aConfig().withUser("testUser", "testPassword"))
		.withPerMethodLifecycle(true);

	@Autowired
	private EmailAuthService emailAuthService;

	@Autowired
	private AuthRedisRepository authRedisRepository;

	@Autowired
	private JavaMailSenderImpl mailSender;

	@BeforeEach
	void setUp() {
		greenMail.start();
		mailSender.setPort(greenMail.getSmtp().getPort());
	}

	@AfterEach
	void tearDown() {
		greenMail.stop();
	}

	@Test
	@DisplayName("이메일 인증 코드 전송에 성공한다.")
	void sendAuthCode_success() {
		//given
		String email = "test@example.com";

		//when
		SendCodeResponse response = emailAuthService.sendAuthCode(email);

		//then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(email);

		assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue(); // 이메일 전송 확인

		// 레디스에 인증 코드 저장 확인
		String savedAuthCode = authRedisRepository.getAuthCode(email);
		assertThat(savedAuthCode).isNotNull();

		// 이메일 내용에 인증 코드를 포함하는지 확인
		String content = GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]);
		assertThat(content).contains(savedAuthCode);
	}

	@Test
	@DisplayName("올바른 인증 코드로 검증에 성공한다.")
	void verifyAuthCode_success() {
		//given
		String email = "test@example.com";
		String authCode = "123456";

		authRedisRepository.saveAuthCode(email, authCode);

		//when
		boolean isVerified = emailAuthService.verifyAuthCode(email, authCode);

		//then
		assertThat(isVerified).isTrue();

		// 검증 완료 후 레디스 상에 인증 코드가 남아 있지 않은지 확인
		String redisAuthCode = authRedisRepository.getAuthCode(email);
		assertThat(redisAuthCode).isNull();
	}

	@Test
	@DisplayName("인증 코드가 일치하지 않으면 검증에 실패한다.")
	void verifyAuthCode_fail() {
		//given
		String email = "test@example.com";
		String correctAuthCode = "123456";
		String wrongAuthCode = "111111";

		authRedisRepository.saveAuthCode(email, correctAuthCode);

		//when
		boolean isVerified = emailAuthService.verifyAuthCode(email, wrongAuthCode);

		//then
		assertThat(isVerified).isFalse();

		// 검증 실패 시에도 레디스에 인증 코드 유효
		String redisAuthCode = authRedisRepository.getAuthCode(email);
		assertThat(redisAuthCode).isEqualTo(correctAuthCode);
	}

}
