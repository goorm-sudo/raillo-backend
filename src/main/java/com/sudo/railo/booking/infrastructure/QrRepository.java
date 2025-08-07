package com.sudo.railo.booking.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sudo.railo.booking.domain.Qr;

public interface QrRepository extends JpaRepository<Qr, Long> {
}
