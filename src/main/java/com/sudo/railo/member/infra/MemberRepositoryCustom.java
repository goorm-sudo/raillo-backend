package com.sudo.railo.member.infra;

import java.util.Optional;

import com.sudo.railo.member.domain.Member;

public interface MemberRepositoryCustom {
	Optional<Member> findByMemberNo(String memberNo);

	Optional<Member> findMemberByNameAndPhoneNumber(String name, String phoneNumber);
}
