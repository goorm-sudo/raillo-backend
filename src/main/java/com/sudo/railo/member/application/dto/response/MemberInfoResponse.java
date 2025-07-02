package com.sudo.railo.member.application.dto.response;

import java.time.LocalDate;

import com.sudo.railo.member.domain.MemberDetail;
import com.sudo.railo.member.domain.Membership;
import com.sudo.railo.member.domain.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 정보 응답 DTO")
public record MemberInfoResponse(

	@Schema(description = "회원 이름", example = "홍길동")
	String name,

	@Schema(description = "회원 연락처", example = "01012345678")
	String phoneNumber,

	@Schema(description = "회원 역할", example = "MEMBER")
	Role role,

	@Schema(description = "회원 상세 정보")
	MemberDetailInfo memberDetailInfo
) {

	// memberDetail -> memberDetailInfo 변환 후 MemberInfoResponse 생성
	public static MemberInfoResponse of(String name, String phoneNumber, MemberDetail memberDetail) {

		MemberDetailInfo memberDetailInfo = new MemberDetailInfo(
			memberDetail.getMemberNo(),
			memberDetail.getMembership(),
			memberDetail.getEmail(),
			memberDetail.getBirthDate(),
			memberDetail.getGender(),
			memberDetail.getTotalMileage()
		);

		return new MemberInfoResponse(name, phoneNumber, Role.MEMBER, memberDetailInfo);
	}

	@Schema(description = "회원 상세 정보")
	public record MemberDetailInfo(

		@Schema(description = "회원 번호", example = "202507020001")
		String memberNo,

		@Schema(description = "회원 등급", example = "BUSINESS")
		Membership membership,

		@Schema(description = "회원 이메일", example = "honggildong@example.com")
		String email,

		@Schema(description = "회원 생년월일", example = "1990-01-01")
		LocalDate birthDate,

		@Schema(description = "회원 성별", example = "M")
		String gender,

		@Schema(description = "총 마일리지", example = "2000")
		Long totalMileage
	) {
	}
}
