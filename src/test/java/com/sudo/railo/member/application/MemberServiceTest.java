package com.sudo.railo.member.application;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sudo.railo.auth.application.AuthService;
import com.sudo.railo.auth.application.dto.request.LoginRequest;
import com.sudo.railo.auth.application.dto.response.TokenResponse;
import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.application.dto.response.MemberInfoResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infrastructure.MemberRepository;
import com.sudo.railo.support.annotation.ServiceTest;
import com.sudo.railo.support.fixture.MemberFixture;

@ServiceTest
class MemberServiceTest {

	@Autowired
	private MemberService memberService;

	@Autowired
	private AuthService authService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Test
	@DisplayName("비회원 등록에 성공한다.")
	void guestRegister_success() {
		//given
		GuestRegisterRequest request = new GuestRegisterRequest("김이름", "01012341234", "testPwd");

		//when
		GuestRegisterResponse response = memberService.guestRegister(request);

		//then
		List<Member> members = memberRepository.findByNameAndPhoneNumber(request.name(), request.phoneNumber());

		assertThat(members).isNotEmpty();

		Member savedGuestMember = members.stream()
			.filter(member -> passwordEncoder.matches(request.password(), member.getPassword()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("등록된 회원을 찾을 수 없습니다."));

		assertThat(response.name()).isEqualTo(request.name());
		assertThat(response.role()).isEqualTo(Role.GUEST);

		assertThat(savedGuestMember.getName()).isEqualTo(request.name());
		assertThat(savedGuestMember.getPhoneNumber()).isEqualTo(request.phoneNumber());
		assertThat(passwordEncoder.matches(request.password(), savedGuestMember.getPassword())).isTrue();
		assertThat(savedGuestMember.getRole()).isEqualTo(Role.GUEST);
	}

	@Test
	@DisplayName("중복된 비회원 정보로 비회원 등록에 실패한다.")
	void guestRegister_fail() {
		//given
		GuestRegisterRequest request = new GuestRegisterRequest("김이름", "01012341234", "testPwd");
		memberService.guestRegister(request);

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberService.guestRegister(request))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.DUPLICATE_GUEST_INFO));
	}

	@Test
	@DisplayName("회원 삭제에 성공한다.")
	void deleteMember_success() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");
		TokenResponse response = authService.login(request);

		String accessToken = response.accessToken();

		//when
		memberService.memberDelete(accessToken, memberNo);

		//then
		Member deletedMember = memberRepository.findByMemberNoIgnoreIsDeleted(memberNo)
			.orElseThrow(() -> new AssertionError("회원을 찾을 수 없습니다."));
		assertThat(deletedMember.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 회원 삭제 시 예외가 발생해 실패한다.")
	void deleteMember_fail_when_user_not_found() {
		//given
		Member member = createMemberWithEncryptedPassword();
		String memberNo = member.getMemberDetail().getMemberNo();

		LoginRequest request = new LoginRequest(memberNo, "testPassword");
		TokenResponse response = authService.login(request);

		String accessToken = response.accessToken();
		String nonExistMemberNo = "202507309999";

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberService.memberDelete(accessToken, nonExistMemberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("회원의 정보 조회에 성공한다.")
	void getMemberInfo_success() {
		//given
		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);

		String memberNo = "202507300001";

		//when
		MemberInfoResponse response = memberService.getMemberInfo(memberNo);

		//then
		assertThat(response.name()).isEqualTo(member.getName());
		assertThat(response.phoneNumber()).isEqualTo(member.getPhoneNumber());
		assertThat(response.role()).isEqualTo(member.getRole());

		MemberInfoResponse.MemberDetailInfo detailInfo = response.memberDetailInfo();
		assertThat(detailInfo.memberNo()).isEqualTo(member.getMemberDetail().getMemberNo());
		assertThat(detailInfo.membership()).isEqualTo(member.getMemberDetail().getMembership());
		assertThat(detailInfo.email()).isEqualTo(member.getMemberDetail().getEmail());
		assertThat(detailInfo.birthDate()).isEqualTo(member.getMemberDetail().getBirthDate().toString());
		assertThat(detailInfo.gender()).isEqualTo(member.getMemberDetail().getGender());
		assertThat(detailInfo.totalMileage()).isEqualTo(member.getMemberDetail().getTotalMileage());
	}

	@Test
	@DisplayName("회원을 찾을 수 없어 회원 정보 조회에 실패한다.")
	void getMemberInfo_fail() {
		//given
		String memberNo = "202507300001";

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberService.getMemberInfo(memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("회원 이메일 조회에 성공한다.")
	void getMemberEmail_success() {
		//given
		Member member = MemberFixture.createStandardMember();
		memberRepository.save(member);

		String memberNo = "202507300001";

		//when
		String memberEmail = memberService.getMemberEmail(memberNo);

		//then
		assertThat(memberEmail).isEqualTo(member.getMemberDetail().getEmail());
	}

	@Test
	@DisplayName("회원을 찾을 수 없어 이메일 조회에 실패한다.")
	void getMemberEmail_fail() {
		//given
		String memberNo = "202507300001";

		//when & then
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> memberService.getMemberEmail(memberNo))
			.satisfies(exception ->
				assertThat(exception.getErrorCode()).isEqualTo(MemberError.USER_NOT_FOUND));
	}

	private Member createMemberWithEncryptedPassword() {
		Member member = MemberFixture.createStandardMember();
		String plainPwd = member.getPassword();
		String encodedPwd = passwordEncoder.encode(plainPwd);

		Member saveMember = Member.create(
			member.getName(),
			member.getPhoneNumber(),
			encodedPwd,
			member.getRole(),
			member.getMemberDetail()
		);

		return memberRepository.save(saveMember);
	}
}
