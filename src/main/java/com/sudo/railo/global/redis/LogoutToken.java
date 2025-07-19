package com.sudo.railo.global.redis;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LogoutToken {

	private String value; // "logout"
	private Duration expireTime; // 토큰 만료 시간
}
