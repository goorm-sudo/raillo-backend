package com.sudo.railo.member.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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

	@Column(columnDefinition = "BIGINT DEFAULT 0")
	@Builder.Default
	private Long totalMileage = 0L;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	@Builder.Default
	private boolean isLocked = false;

	@Column(columnDefinition = "INT DEFAULT 0")
	@Builder.Default
	private int lockCount = 0;

}
