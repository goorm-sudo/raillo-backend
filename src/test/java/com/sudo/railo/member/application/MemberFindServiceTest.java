package com.sudo.railo.member.application;

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
import com.sudo.railo.auth.application.dto.request.VerifyCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.application.dto.response.TemporaryTokenResponse;
import com.sudo.railo.auth.exception.AuthError;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.global.redis.MemberRedisRepository;
import com.sudo.railo.member.application.dto.request.FindMemberNoRequest;
import com.sudo.railo.member.application.dto.request.FindPasswordRequest;
import com.sudo.railo.member.application.dto.response.VerifyMemberNoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;

@ServiceTest
class MemberFindServiceTest {

	@RegisterExtension
	static GreenMailExtension greenMail = new GreenMailExtension(
		new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP) // 포트 충돌이 있어 동적 포트 할당
	)
		.withConfiguration(GreenMailConfiguration.aConfig().withUser("testUser", "testPassword"))
		.withPerMethodLifecycle(true);

	@Autowired
	private JavaMailSenderImpl mailSender;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberFindService memberFindService;

	@Autowired
	private MemberRedisRepository memberRedisRepository;

	private Member member;
	@Autowired
	private AuthRedisRepository authRedisRepository;

	@BeforeEach
	void setUp() {
		greenMail.start();
		mailSender.setPort(greenMail.getSmtp().getPort());

		member = MemberFixture.createStandardMember();
		memberRepository.save(member);
	}

	@AfterEach
	void tearDown() {
		greenMail.stop();
	}

	@Test
	@DisplayName("존재하는 회원 정보로 이메일 인증을 통한 회원번호 찾기 요청에 성공한다.")
	void requestFindMemberNo_success() {
		//given
		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();
		FindMemberNoRequest request = new FindMemberNoRequest(member.getName(), member.getPhoneNumber());

		//when
		SendCodeResponse response = memberFindService.requestFindMemberNo(request);

		//then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(memberEmail);

		assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

		String savedMemberNo = memberRedisRepository.getMemberNo(memberEmail);
		assertThat(savedMemberNo).isEqualTo(memberNo);

		String mailContent = GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]);
		String authCode = authRedisRepository.getAuthCode(memberEmail);
		assertThat(mailContent).contains(authCode);
	}

	@Test
	@DisplayName("존재하지 않는 회원의 정보로 회원 번호를 찾으면 요청에 실패한다.")
	void requestFindMemberNo_fail() {
		//given
		FindMemberNoRequest request = new FindMemberNoRequest("존재하지않는이름", "01099998888");

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberFindService.requestFindMemberNo(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("올바른 인증 코드로 회원 번호 검증 요청에 성공한다.")
	void verifyAuthCode_success() {
		//given
		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();
		String authCode = "123456";

		memberRedisRepository.saveMemberNo(memberEmail, memberNo);
		authRedisRepository.saveAuthCode(memberEmail, authCode);

		VerifyCodeRequest request = new VerifyCodeRequest(memberEmail, authCode);

		//when
		VerifyMemberNoResponse response = memberFindService.verifyFindMemberNo(request);

		//then
		assertThat(response).isNotNull();
		assertThat(response.memberNo()).isEqualTo(memberNo);

		// 검증 성공 후 레디스에 저장된 회원 번호와 인증 코드 삭제 되었는지 확인
		String savedMemberNo = memberRedisRepository.getMemberNo(memberEmail);
		assertThat(savedMemberNo).isNull();
		String savedAuthCode = authRedisRepository.getAuthCode(memberEmail);
		assertThat(savedAuthCode).isNull();
	}

	@Test
	@DisplayName("인증 코드가 일치하지 않으면 회원번호 검증 요청에 실패한다.")
	void verifyAuthCode_fail() {
		//given
		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();
		String correctAuthCode = "123456";
		String wrongAuthCode = "111111";

		memberRedisRepository.saveMemberNo(memberEmail, memberNo);
		authRedisRepository.saveAuthCode(memberEmail, correctAuthCode);

		VerifyCodeRequest request = new VerifyCodeRequest(memberEmail, wrongAuthCode);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberFindService.verifyFindMemberNo(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthError.INVALID_AUTH_CODE));
	}

	@Test
	@DisplayName("존재하는 회원 정보로 이메일 인증을 통한 비밀번호 찾기 요청에 성공한다.")
	void requestFindPassword_success() {
		//given
		FindPasswordRequest request = new FindPasswordRequest(member.getName(), member.getMemberDetail().getMemberNo());

		//when
		SendCodeResponse response = memberFindService.requestFindPassword(request);

		//then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(member.getMemberDetail().getEmail());

		assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

		String savedAuthCode = authRedisRepository.getAuthCode(member.getMemberDetail().getEmail());
		assertThat(savedAuthCode).isNotNull();

		String mailContent = GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]);
		assertThat(mailContent).contains(savedAuthCode);
	}

	@Test
	@DisplayName("존재하지 않는 회원번호로 비밀번호 찾기 요청 시도 시 실패한다.")
	void requestFindPassword_fail_when_wrong_member_no() {
		//given
		String wrongMemberNo = "202007070001";
		FindPasswordRequest request = new FindPasswordRequest(member.getName(), wrongMemberNo);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberFindService.requestFindPassword(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("회원 정보에 있는 이름과 일치하지 않는 요청일 경우 실패한다.")
	void requestFindPassword_fail_when_miss_match_name() {
		//given
		String wrongName = "다른이름";
		FindPasswordRequest request = new FindPasswordRequest(wrongName, member.getMemberDetail().getMemberNo());

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberFindService.requestFindPassword(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.NAME_MISMATCH));
	}

	@Test
	@DisplayName("올바른 인증 코드로 비밀번호 찾기 검증에 성공하면 임시 토큰이 발급된다.")
	void verifyFindPassword_success() {
		//given
		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();
		String authCode = "123456";

		memberRedisRepository.saveMemberNo(memberEmail, memberNo);
		authRedisRepository.saveAuthCode(memberEmail, authCode);

		VerifyCodeRequest request = new VerifyCodeRequest(memberEmail, authCode);

		//when
		TemporaryTokenResponse response = memberFindService.verifyFindPassword(request);

		//then
		assertThat(response).isNotNull();
		assertThat(response.temporaryToken()).isNotNull();

		// 검증 후 레디스에 회원번호가 삭제되었는지 확인
		String savedAuthCode = authRedisRepository.getAuthCode(memberEmail);
		assertThat(savedAuthCode).isNull();
	}
	
	@Test
	@DisplayName("인증 코드가 일치하지 않으면 비밀번호 찾기 검증 요청에 실패한다.")
	void verifyFindPassword_fail() {
		//given
		String memberEmail = member.getMemberDetail().getEmail();
		String memberNo = member.getMemberDetail().getMemberNo();
		String correctAuthCode = "123456";
		String wrongAuthCode = "111111";

		memberRedisRepository.saveMemberNo(memberEmail, memberNo);
		authRedisRepository.saveAuthCode(memberEmail, correctAuthCode);

		VerifyCodeRequest request = new VerifyCodeRequest(memberEmail, wrongAuthCode);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberFindService.verifyFindPassword(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthError.INVALID_AUTH_CODE));
	}
}
