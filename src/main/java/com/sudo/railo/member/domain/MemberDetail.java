package com.sudo.railo.member.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberDetail {

	@Column(unique = true)
	private String memberNo;

	@Enumerated(EnumType.STRING)
	private Membership membership;

	@Column(unique = true)
	private String email;

	private LocalDate birthDate;

	@Column(length = 1)
	private String gender;

	private Long totalMileage;

	private boolean isLocked;

	private int lockCount;

	public static MemberDetail create(String memberNo, Membership membership, String email, LocalDate birthDate,
		String gender) {
		return new MemberDetail(memberNo, membership, email, birthDate, gender, 0L, false, 0);
	}

}
