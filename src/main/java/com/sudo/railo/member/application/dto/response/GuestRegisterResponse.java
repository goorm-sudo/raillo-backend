package com.sudo.railo.member.application.dto.response;

import com.sudo.railo.member.domain.Role;

public record GuestRegisterResponse(
	String name,
	Role role
) {
}
