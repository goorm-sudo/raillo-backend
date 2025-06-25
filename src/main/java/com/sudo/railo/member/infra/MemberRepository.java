package com.sudo.railo.member.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
	boolean existsByMemberDetailEmail(String email);

	List<Member> findByNameAndPhoneNumber(String name, String phoneNumber);
}
