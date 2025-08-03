package com.sudo.railo.support.fixture;

import java.time.LocalDate;

import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;

import lombok.Getter;

@Getter
public enum MemberFixture {

	MEMBER(
		"202507300001",
		Membership.FAMILY,
		"test@example.com",
		LocalDate.of(2000, 1, 1),
		"M",
		"member",
		"010-1111-1111",
		"testPassword",
		Role.MEMBER
	);

	private final String MemberNo;
	private final Membership membership;
	private final String email;
	private final LocalDate birthDate;
	private final String gender;
	private final String name;
	private final String phoneNumber;
	private final String password;
	private final Role role;

	MemberFixture(String memberNo, Membership membership, String email, LocalDate birthDate, String gender,
		String name, String phoneNumber, String password, Role role) {
		MemberNo = memberNo;
		this.membership = membership;
		this.email = email;
		this.birthDate = birthDate;
		this.gender = gender;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.password = password;
		this.role = role;
	}

	public static Member createStandardMember() {
		MemberDetail memberDetail = MemberDetail.create(MEMBER.MemberNo, MEMBER.membership, MEMBER.email, MEMBER.birthDate, MEMBER.gender);
		return Member.create(MEMBER.name, MEMBER.phoneNumber, MEMBER.password, MEMBER.role, memberDetail);
	}
}
