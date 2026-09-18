package com.tradehub;

import com.tradehub.auth.AuthService;
import com.tradehub.auth.JwtService;
import com.tradehub.auth.dto.LoginRequest;
import com.tradehub.auth.dto.LoginResponse;
import com.tradehub.auth.dto.RegisterRequest;
import com.tradehub.auth.dto.RegisterResponse;
import com.tradehub.auth.dto.UserProfileResponse;
import com.tradehub.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthFlowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Test
    void registerLoginAndFetchProfile() {
        String email = "flow-" + UUID.randomUUID() + "@test.com";
        String password = "Passw0rd!";

        RegisterResponse registered = authService.register(
                new RegisterRequest("Flow Tester", email, password));
        assertThat(registered.email()).isEqualTo(email);
        assertThat(registered.fullName()).isEqualTo("Flow Tester");

        LoginResponse login = authService.login(
                new LoginRequest(email, password));
        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.accessToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(login.accessToken())).isTrue();
        assertThat(jwtService.extractEmail(login.accessToken())).isEqualTo(email);

        UserProfileResponse profile = authService.getCurrentUser(email);
        assertThat(profile.email()).isEqualTo(email);
        assertThat(profile.roles()).contains(com.tradehub.user.RoleName.CUSTOMER);
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        String email = "wrong-" + UUID.randomUUID() + "@test.com";

        authService.register(
                new RegisterRequest("Wrong Tester", email, "Correct@1"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(email, "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}