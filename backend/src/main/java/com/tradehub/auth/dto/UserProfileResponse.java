package com.tradehub.auth.dto;

import com.tradehub.user.RoleName;

import java.time.LocalDateTime;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        Set<RoleName> roles,
        LocalDateTime createdAt
) {
}