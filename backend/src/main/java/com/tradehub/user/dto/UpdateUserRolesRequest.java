package com.tradehub.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.tradehub.user.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(

        @NotNull(message = "Roles are required")
        @NotEmpty(message = "At least one role is required")
        Set<RoleName> roles
) {
}