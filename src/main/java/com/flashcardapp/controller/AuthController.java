package com.flashcardapp.controller;

import com.flashcardapp.dto.AuthFlowResponse;
import com.flashcardapp.dto.AuthRequest;
import com.flashcardapp.dto.AuthResponse;
import com.flashcardapp.dto.CsrfTokenResponse;
import com.flashcardapp.dto.OtpResendRequest;
import com.flashcardapp.dto.OtpVerifyRequest;
import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.dto.UserProfileResponse;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.AuthMethod;
import com.flashcardapp.entity.OtpPurpose;
import com.flashcardapp.service.AuthSessionService;
import com.flashcardapp.service.OtpService;
import com.flashcardapp.service.RegistrationFlowService;
import com.flashcardapp.service.TrustedDeviceService;
import com.flashcardapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final RegistrationFlowService registrationFlowService;
    private final TrustedDeviceService trustedDeviceService;
    private final AuthSessionService authSessionService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          OtpService otpService,
                          RegistrationFlowService registrationFlowService,
                          TrustedDeviceService trustedDeviceService,
                          AuthSessionService authSessionService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.registrationFlowService = registrationFlowService;
        this.trustedDeviceService = trustedDeviceService;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/register")
    public AuthFlowResponse register(@Valid @RequestBody RegisterRequest request,
                                     HttpServletRequest servletRequest) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp");
        }
        return otpRequired(registrationFlowService.begin(request, servletRequest));
    }

    @PostMapping("/login")
    public AuthFlowResponse login(@Valid @RequestBody AuthRequest request,
                                  HttpServletRequest servletRequest,
                                  HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
        }

        AppUser user = userService.currentUser(userDetails.getUsername());
        if (!user.isEmailVerified()) {
            return otpRequired(otpService.dispatch(user, OtpPurpose.EMAIL_VERIFICATION, servletRequest));
        }
        if (trustedDeviceService.isTrusted(user, servletRequest)) {
            return AuthFlowResponse.authenticated(
                    authSessionService.issueSession(user, AuthMethod.PASSWORD, response)
            );
        }
        return otpRequired(otpService.dispatch(user, OtpPurpose.LOGIN, servletRequest));
    }

    @PostMapping("/otp/verify")
    public AuthFlowResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                      HttpServletRequest servletRequest,
                                      HttpServletResponse response) {
        AppUser user = otpService.verify(request.challengeId(), request.code(), servletRequest);
        if (request.rememberDevice()) {
            trustedDeviceService.remember(user, servletRequest, response);
        }
        return AuthFlowResponse.authenticated(
                authSessionService.issueSession(user, AuthMethod.PASSWORD, response)
        );
    }

    @PostMapping("/otp/resend")
    public AuthFlowResponse resendOtp(@Valid @RequestBody OtpResendRequest request,
                                      HttpServletRequest servletRequest) {
        return otpRequired(otpService.resend(request.challengeId(), servletRequest));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authSessionService.refresh(request, response);
    }

    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        AppUser user = userService.currentUser(authentication.getName());
        return UserProfileResponse.from(user);
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return CsrfTokenResponse.from(csrfToken);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authSessionService.logout(request, response);
    }

    private AuthFlowResponse otpRequired(OtpService.OtpDispatch dispatch) {
        return AuthFlowResponse.otpRequired(
                dispatch.challengeId(),
                dispatch.maskedEmail(),
                dispatch.expiresInSeconds(),
                dispatch.resendAvailableInSeconds(),
                dispatch.remainingSends()
        );
    }
}
