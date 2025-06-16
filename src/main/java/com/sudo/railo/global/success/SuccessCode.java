package com.sudo.railo.global.success;

import org.springframework.http.HttpStatus;

public interface SuccessCode {

	HttpStatus getStatus();

	String getMessage();
}
