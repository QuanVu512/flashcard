package com.flashcardapp.controller;

import com.flashcardapp.dto.AuthRequest;
import com.flashcardapp.dto.AuthResponse;
import com.flashcardapp.dto.CsrfTokenResponse;
import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.dto.UserProfileResponse;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.helper.security.AuthCookieManager;
import com.flashcardapp.helper.security.JwtService;
import com.flashcardapp.service.UserService;
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
    private final JwtService jwtService;
    private final AuthCookieManager authCookieManager;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          AuthCookieManager authCookieManager,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authCookieManager = authCookieManager;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletResponse response) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp");
        }
        AppUser user = userService.registerUser(request);
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        return authenticate(user, userDetails, response);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request,
                              HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
        }
        AppUser user = userService.currentUser(userDetails.getUsername());
        return authenticate(user, userDetails, response);
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
    public void logout(HttpServletResponse response) {
        authCookieManager.clearAccessToken(response);
    }

    private AuthResponse authenticate(AppUser user,
                                      UserDetails userDetails,
                                      HttpServletResponse response) {
        long expiresInSeconds = jwtService.expirationSeconds();
        authCookieManager.writeAccessToken(response, jwtService.generateToken(userDetails), expiresInSeconds);
        return new AuthResponse(
                expiresInSeconds,
                UserProfileResponse.from(user)
        );
    }
}
