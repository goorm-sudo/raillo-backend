package com.sudo.railo.booking.domain;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sudo.railo.global.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Qr extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "qr_id")
	private Long id;

	@Column(nullable = false)
	private boolean isUsable;

	@Column(nullable = false)
	private int scanCount;

	@Column(nullable = true)
	private String qrUrl;
}
