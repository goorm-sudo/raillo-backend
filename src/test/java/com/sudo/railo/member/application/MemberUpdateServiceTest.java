package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sudo.railo.global.exception.error.BusinessException;
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

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberUpdateService memberUpdateService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	private Member member;

	@BeforeEach
	void setUp() {
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
		MemberDetail otherMemberDetail = MemberDetail.create("202507300002", Membership.BUSINESS, "test2@example.com",
			LocalDate.of(2000, 2, 2), "W");
		Member otherMember = Member.create("김구름", "01088889999", "testPwd", Role.MEMBER, otherMemberDetail);
		memberRepository.save(otherMember);

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
