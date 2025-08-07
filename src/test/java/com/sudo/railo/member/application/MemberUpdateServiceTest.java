package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.sudo.railo.auth.application.dto.request.SendCodeRequest;
import com.sudo.railo.auth.application.dto.response.SendCodeResponse;
import com.sudo.railo.auth.exception.AuthError;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.global.redis.AuthRedisRepository;
import com.sudo.railo.global.redis.MemberRedisRepository;
import com.sudo.railo.member.application.dto.request.UpdateEmailRequest;
import com.sudo.railo.member.application.dto.request.UpdatePasswordRequest;
import com.sudo.railo.member.application.dto.request.UpdatePhoneNumberRequest;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;

@ServiceTest
class MemberUpdateServiceTest {

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
	private MemberUpdateService memberUpdateService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private AuthRedisRepository authRedisRepository;

	@Autowired
	private MemberRedisRepository memberRedisRepository;

	private Member member;
	private Member otherMember;

	@BeforeEach
	void setUp() {
		greenMail.start();
		mailSender.setPort(greenMail.getSmtp().getPort());

		member = MemberFixture.createStandardMember();
		String plainPwd = member.getPassword();
		String encodedPwd = passwordEncoder.encode(plainPwd);

		Member saveMember = Member.create(
			member.getName(),
			member.getPhoneNumber(),
			encodedPwd,
			member.getRole(),
			member.getMemberDetail()
		);
		memberRepository.save(saveMember);

		MemberDetail otherMemberDetail = MemberDetail.create("202507300002", Membership.BUSINESS, "test2@example.com",
			LocalDate.of(2000, 2, 2), "W");
		otherMember = Member.create("김구름", "01088889999", "testPwd", Role.MEMBER, otherMemberDetail);
		memberRepository.save(otherMember);

	}

	@AfterEach
	void tearDown() {
		greenMail.stop();
	}

	@Test
	@DisplayName("이메일 인증을 통한 이메일 변경 요청에 성공한다.")
	void requestUpdateEmail_success() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String newEmail = "newEmail@example.com";
		SendCodeRequest request = new SendCodeRequest(newEmail);

		//when
		SendCodeResponse response = memberUpdateService.requestUpdateEmail(request, memberNo);

		//then
		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(newEmail);

		assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue(); // 이메일 전송 확인

		// 레디스에 인증 코드 저장 확인
		String savedAuthCode = authRedisRepository.getAuthCode(newEmail);
		assertThat(savedAuthCode).isNotNull();

		// 레디스에 요청이 저장되었는지 확인
		boolean result = memberRedisRepository.handleUpdateEmailRequest(newEmail);
		assertThat(result).isFalse(); // 요청이 이미 저장되어 있기 떄문에 false

		// 이메일 내용에 인증 코드를 포함하는지 확인
		String content = GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]);
		assertThat(content).contains(savedAuthCode);
	}

	@Test
	@DisplayName("존재하지 않는 회원으로 이메일 변경 요청 시 요청에 실패한다.")
	void requestUpdateEmail_fail_when_user_not_found() {
		//given
		String wrongMemberNo = "202007079999";
		String newEmail = "newEmail@example.com";
		SendCodeRequest request = new SendCodeRequest(newEmail);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.requestUpdateEmail(request, wrongMemberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("기존 이메일과 동일한 이메일로 변경 요청 시 변경에 실패한다.")
	void requestUpdateEmail_fail_when_same_email() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String sameEmail = member.getMemberDetail().getEmail();
		SendCodeRequest request = new SendCodeRequest(sameEmail);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.requestUpdateEmail(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_EMAIL));
	}

	@Test
	@DisplayName("다른 회원이 사용 중인 이메일로 변경 요청 시 변경에 실패한다.")
	void requestUpdateEmail_fail_when_duplicate_email() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String duplicateEmail = otherMember.getMemberDetail().getEmail();
		SendCodeRequest request = new SendCodeRequest(duplicateEmail);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.requestUpdateEmail(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_EMAIL));
	}

	@Test
	@DisplayName("동일 이메일에 대한 변경 요청이 이미 있는 경우 이메일 변경에 실패한다.")
	void requestUpdateEmail_fail_when_already_requested() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String newEmail = "newEmail@example.com";
		SendCodeRequest request = new SendCodeRequest(newEmail);

		// 레디스에 이미 요청이 있는 것으로 가정 후 미리 저장
		memberRedisRepository.handleUpdateEmailRequest(newEmail);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.requestUpdateEmail(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.EMAIL_UPDATE_ALREADY_REQUESTED));
	}

	@Test
	@DisplayName("이메일 인증을 통한 이메일 변경 요청 검증에 성공한다.")
	void verifyUpdateEmail_success() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String newEmail = "newEmail@example.com";
		String authCode = "123456";

		authRedisRepository.saveAuthCode(newEmail, authCode);
		memberRedisRepository.handleUpdateEmailRequest(newEmail);

		UpdateEmailRequest request = new UpdateEmailRequest(newEmail, authCode);

		//when
		memberUpdateService.verifyUpdateEmail(request, memberNo);

		//then
		Member updatedMember = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new AssertionError("DB에 저장된 회원을 찾을 수 없습니다."));
		assertThat(updatedMember.getMemberDetail().getEmail()).isEqualTo(newEmail);

		// 레디스에 인증 코드 삭제 되었는지 확인
		String redisAuthCode = authRedisRepository.getAuthCode(newEmail);
		assertThat(redisAuthCode).isNull();

		// 레디스에 이메일 변경 요청 삭제 확인
		boolean result = memberRedisRepository.handleUpdateEmailRequest(newEmail);
		assertThat(result).isTrue(); // 삭제되면 다시 요청이 가능하여 true
	}

	@Test
	@DisplayName("존재하지 않는 회원으로 이메일 변경 검증 시 요청에 실패한다.")
	void verifyUpdateEmail_fail_when_user_not_found() {
		//given
		String wrongMemberNo = "202007079999";
		String newEmail = "newEmail@example.com";
		String authCode = "123456";

		UpdateEmailRequest request = new UpdateEmailRequest(newEmail, authCode);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.verifyUpdateEmail(request, wrongMemberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("잘못된 인증 코드로 이메일 변경 검증 시 요청에 실패한다.")
	void verifyUpdateEmail_fail_when_wrong_auth_code() {
		//given
		String correctAuthCode = "123456";
		String wrongAuthCode = "999999";
		String newEmail = "newEmail@example.com";
		String memberNo = member.getMemberDetail().getMemberNo();

		authRedisRepository.saveAuthCode(newEmail, correctAuthCode);

		UpdateEmailRequest request = new UpdateEmailRequest(newEmail, wrongAuthCode);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.verifyUpdateEmail(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(AuthError.INVALID_AUTH_CODE));
	}

	@Test
	@DisplayName("유효한 휴대폰 번호로 변경 요청 시 성공한다.")
	void updatePhoneNumber_success() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String newPhoneNumber = "01022222222";
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(newPhoneNumber);

		//when
		memberUpdateService.updatePhoneNumber(request, memberNo);

		//then
		Member updatedMember = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new AssertionError("DB에 저장된 회원을 찾을 수 없습니다."));
		assertThat(updatedMember.getPhoneNumber()).isEqualTo(newPhoneNumber);
	}

	@Test
	@DisplayName("회원번호로 일치하는 회원을 찾을 수 없으면 휴대폰 번호 변경 요청에 실패한다.")
	void updatePhoneNumber_fail_when_wrong_member_no() {
		//given
		String wrongMemberNo = "202007070001";
		String newPhoneNumber = "01022222222";

		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(newPhoneNumber);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.updatePhoneNumber(request, wrongMemberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("이미 본인과 같은 휴대폰 번호로 변경 요청 시 요청이 실패한다.")
	void updatePhoneNumber_fail_when_same_phone_number() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String samePhoneNumber = member.getPhoneNumber();

		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(samePhoneNumber);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.updatePhoneNumber(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_PHONE_NUMBER));
	}

	@Test
	@DisplayName("다른 회원이 사용하고 있는 휴대폰 번호로 변경 요청 시 요청이 실패한다.")
	void updatePhoneNumber_fail_when_duplicate_phone_number() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String duplicatePhoneNumber = otherMember.getPhoneNumber();
		UpdatePhoneNumberRequest request = new UpdatePhoneNumberRequest(duplicatePhoneNumber);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.updatePhoneNumber(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_PHONE_NUMBER));
	}

	@Test
	@DisplayName("유효한 비밀번호로 변경 요청 시 변경에 성공한다.")
	void updatePassword_success() {
		//given
		String newPassword = "newPassword";
		String memberNo = member.getMemberDetail().getMemberNo();
		UpdatePasswordRequest request = new UpdatePasswordRequest(newPassword);

		//when
		memberUpdateService.updatePassword(request, memberNo);

		//then
		Member updatedMember = memberRepository.findByMemberNo(memberNo)
			.orElseThrow(() -> new AssertionError("DB에 저장된 회원을 찾을 수 없습니다."));
		assertThat(passwordEncoder.matches(newPassword, updatedMember.getPassword())).isTrue();
	}

	@Test
	@DisplayName("회원번호로 회원을 찾을 수 없으면 비밀번호 변경에 실패한다.")
	void updatePassword_fail_when_wrong_member_no() {
		//given
		String wrongMemberNo = "202507300009";
		String newPassword = "newPassword";
		UpdatePasswordRequest request = new UpdatePasswordRequest(newPassword);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.updatePassword(request, wrongMemberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("이미 동일한 비밀번호로 변경 요청 시 비밀번호 변경에 실패한다.")
	void updatePassword_fail_when_same_password() {
		//given
		String memberNo = member.getMemberDetail().getMemberNo();
		String samePassword = member.getPassword();
		UpdatePasswordRequest request = new UpdatePasswordRequest(samePassword);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberUpdateService.updatePassword(request, memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.SAME_PASSWORD));
	}
}
