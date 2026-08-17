package com.flashcardapp.helper.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Optional<String> currentUsername() {
        return currentAuthentication().map(SecurityUtil::extractUsername);
    }

    public static List<String> currentAuthorities() {
        return currentAuthentication()
                .map(authentication -> authentication.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList())
                .orElseGet(List::of);
    }

    public static boolean isLoggedIn() {
        return currentAuthentication().isPresent();
    }

    public static boolean isAdmin() {
        return currentAuthorities().contains("ROLE_ADMIN");
    }

    public static Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    private static String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String username) {
            return username;
        }
        return authentication.getName();
    }
}
