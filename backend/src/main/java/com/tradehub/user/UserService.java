package com.tradehub.user;

import com.tradehub.auth.dto.UserProfileResponse;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.user.dto.UpdateUserRolesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserProfileResponse updateRoles(
            Long userId,
            UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRoles(new HashSet<>(request.roles()));
        User savedUser = userRepository.save(user);

        return toProfile(savedUser);
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt());
    }
}