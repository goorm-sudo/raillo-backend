package com.sudo.railo.member.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sudo.railo.global.exception.error.BusinessException;
import com.sudo.railo.member.application.dto.request.GuestRegisterRequest;
import com.sudo.railo.member.application.dto.response.GuestRegisterResponse;
import com.sudo.railo.member.domain.Member;
import com.sudo.railo.member.domain.Role;
import com.sudo.railo.member.exception.MemberError;
import com.sudo.railo.member.infra.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public GuestRegisterResponse guestRegister(GuestRegisterRequest request) {

		// 중복 체크
		if (memberRepository.existsByNameAndPhoneNumber(request.name(), request.phoneNumber())) {
			Member member = memberRepository.findByNameAndPhoneNumber(request.name(), request.phoneNumber());

			if (passwordEncoder.matches(request.password(), member.getPassword())) {
				throw new BusinessException(MemberError.DUPLICATE_GUEST_INFO);
			}
		}

		String encodedPassword = passwordEncoder.encode(request.password());

		Member member = Member.create(request.name(), request.phoneNumber(), encodedPassword, Role.GUEST, null);
		memberRepository.save(member);

		return new GuestRegisterResponse(request.name(), Role.GUEST);
	}
}
