package com.sudo.railo.member.infra;

import java.util.Optional;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.QMember;
import com.sudo.railo.member.domain.QMemberDetail;
import com.sudo.railo.member.domain.Role;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Member> findByMemberNo(String memberNo) {

		QMember member = QMember.member;
		QMemberDetail memberDetail = QMemberDetail.memberDetail;

		Member result = queryFactory
			.selectFrom(member)
			.where(member.memberDetail.memberNo.eq(memberNo))
			.fetchOne();
		return Optional.ofNullable(result);
	}

	@Override
	public Optional<Member> findMemberByNameAndPhoneNumber(String name, String phoneNumber) {

		QMember member = QMember.member;

		return Optional.ofNullable(queryFactory
			.selectFrom(member)
			.where(
				member.name.eq(name),
				member.phoneNumber.eq(phoneNumber),
				member.role.eq(Role.MEMBER)
			)
			.fetchOne());
	}
}
