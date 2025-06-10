package com.sudo.railo.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Embeddable
@NoArgsConstructor
public class MemberDetail {

    private String memberNo;

    @Enumerated(EnumType.STRING)
    private Membership membership;

    private String email;

    private LocalDate birthDate;

    @Column(length = 1)
    private String gender;

    private Long totalMileage;

    @Column(length = 1)
    private String isLocked;

    private int lockCount;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


}
