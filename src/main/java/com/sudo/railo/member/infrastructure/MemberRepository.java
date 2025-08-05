package com.sudo.railo.member.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sudo.railo.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
	boolean existsByMemberDetailEmail(String email);

	List<Member> findByNameAndPhoneNumber(String name, String phoneNumber);

	boolean existsByPhoneNumber(String phoneNumber);

	@Modifying
	@Query(value = "delete from member where id in (:memberIds)", nativeQuery = true)
	void deleteAllByIdInBatch(@Param("memberIds") List<Long> memberIds);
}
