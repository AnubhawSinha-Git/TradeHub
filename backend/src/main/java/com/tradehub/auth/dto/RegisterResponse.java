package com.tradehub.auth.dto;

import com.tradehub.user.RoleName;

import java.util.Set;

public record RegisterResponse(
        Long id,
        String fullName,
        String email,
        Set<RoleName> roles
) {
}