package com.sudo.railo.member.application.dto.response;

import java.time.LocalDate;

import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;

public record MemberInfoResponse(
	String name,
	String phoneNumber,
	Role role,
	MemberDetailInfo memberDetailInfo
) {

	// memberDetail -> memberDetailInfo 변환 후 MemberInfoResponse 생성
	public static MemberInfoResponse of(String name, String phoneNumber, Role role, MemberDetail memberDetail) {

		MemberDetailInfo memberDetailInfo = new MemberDetailInfo(
			memberDetail.getMemberNo(),
			memberDetail.getMembership(),
			memberDetail.getEmail(),
			memberDetail.getBirthDate(),
			memberDetail.getGender(),
			memberDetail.getTotalMileage()
		);

		return new MemberInfoResponse(name, phoneNumber, role, memberDetailInfo);
	}

	public record MemberDetailInfo(
		String memberNo,
		Membership membership,
		String email,
		LocalDate birthDate,
		String gender,
		Long totalMileage
	) {
	}
}
