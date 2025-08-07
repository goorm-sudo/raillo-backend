package com.sudo.railo.member.infrastructure;

import java.util.Optional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;

public interface MemberRepositoryCustom {
	Optional<Member> findByMemberNo(String memberNo);

	Optional<Member> findMemberByNameAndPhoneNumber(String name, String phoneNumber);

	default Member getMember(String memberNo) {
		return findByMemberNo(memberNo)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
	}
}
