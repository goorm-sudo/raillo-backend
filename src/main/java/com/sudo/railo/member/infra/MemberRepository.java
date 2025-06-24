package com.sudo.railo.member.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
	boolean existsByMemberDetailEmail(String email);

	boolean existsByNameAndPhoneNumber(String name, String phoneNumber);

	Member findByNameAndPhoneNumber(String name, String phoneNumber);
}
