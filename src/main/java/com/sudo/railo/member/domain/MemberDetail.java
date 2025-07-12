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

	public void updateEmail(String newEmail) {
		this.email = newEmail;
	}

	/**
	 * 마일리지 조회 (MemberInfoAdapter 호환성을 위한 메서드)
	 */
	public Long getMileage() {
		return this.totalMileage;
	}

	/**
	 * 마일리지 추가
	 */
	public void addMileage(Long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("마일리지 추가 금액은 0보다 커야 합니다");
		}
		this.totalMileage += amount;
	}

	/**
	 * 마일리지 차감
	 */
	public void useMileage(Long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("마일리지 사용 금액은 0보다 커야 합니다");
		}
		if (this.totalMileage < amount) {
			throw new IllegalArgumentException("마일리지가 부족합니다");
		}
		this.totalMileage -= amount;
	}

}
