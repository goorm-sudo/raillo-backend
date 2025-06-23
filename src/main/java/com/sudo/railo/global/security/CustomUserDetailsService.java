package com.sudo.railo.global.security;

import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final MemberRepository memberRepository;

	@Override
	public UserDetails loadUserByUsername(String memberNo) {
		log.info("memberNo : {}", memberNo);
		return memberRepository.findByMemberNo(memberNo)
			.map(this::createUserDetails)
			.orElseThrow(() -> new BusinessException(MemberError.USER_NOT_FOUND));
	}

	// userDetails 객체로 만들어 리턴
	private UserDetails createUserDetails(Member member) {

		GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(member.getRole().name());

		return new User(
			member.getMemberDetail().getMemberNo(),
			member.getPassword(),
			Collections.singleton(grantedAuthority)
		);
	}
}
