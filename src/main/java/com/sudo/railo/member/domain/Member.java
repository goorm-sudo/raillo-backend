package com.sudo.railo.member.domain;

import com.sudo.railo.global.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String phoneNumber;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Embedded
	private MemberDetail memberDetail;

	private Member(String name, String phoneNumber, String password, Role role, MemberDetail memberDetail) {
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.password = password;
		this.role = role;
		this.memberDetail = memberDetail;
	}

	public static Member create(String name, String phoneNumber, String password, Role role,
		MemberDetail memberDetail) {
		return new Member(name, phoneNumber, password, role, memberDetail);
	}

	// 비회원 등록 정적 팩토리 메서드
	public static Member guestCreate(String name, String phoneNumber, String password) {
		return new Member(name, phoneNumber, password, Role.GUEST, null);
	}

	public void updatePhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	/**
	 * 마일리지 추가
	 */
	public void addMileage(Long amount) {
		if (this.memberDetail == null) {
			throw new IllegalStateException("회원 상세 정보가 없습니다");
		}
		this.memberDetail.addMileage(amount);
	}

	/**
	 * 마일리지 차감
	 */
	public void useMileage(Long amount) {
		if (this.memberDetail == null) {
			throw new IllegalStateException("회원 상세 정보가 없습니다");
		}
		this.memberDetail.useMileage(amount);
	}

	/**
	 * 현재 마일리지 조회
	 */
	public Long getTotalMileage() {
		if (this.memberDetail == null) {
			return 0L;
		}
		return this.memberDetail.getTotalMileage();
	}
}
