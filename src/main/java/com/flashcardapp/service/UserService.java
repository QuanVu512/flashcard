package com.flashcardapp.service;

import com.flashcardapp.dto.RegisterRequest;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.Client;
import com.flashcardapp.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerUser(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("Email đã được sử dụng");
        }

        Client client = new Client();
        client.setDisplayName(request.getDisplayName().trim());

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setClient(client);

        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Client currentClient(String email) {
        Client client = appUserRepository.findWithClientByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung"))
                .getClient();
        client.getDisplayName();
        client.getScore();
        return client;
    }

    @Transactional(readOnly = true)
    public AppUser currentUser(String email) {
        AppUser user = appUserRepository.findWithClientByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung"));
        user.getClient().getDisplayName();
        user.getClient().getScore();
        return user;
    }

    @Transactional
    public void ensureAdminUser(String email, String password, String displayName) {
        if (!hasText(email) || !hasText(password)) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setDisplayName(hasText(displayName) ? displayName.trim() : "Admin");

                    AppUser newUser = new AppUser();
                    newUser.setEmail(normalizedEmail);
                    newUser.setPasswordHash(passwordEncoder.encode(password));
                    newUser.setClient(client);
                    return newUser;
                });

        user.setRole("ROLE_ADMIN");
        user.setEnabled(true);
        appUserRepository.save(user);
    }

    @Transactional
    public long addScore(String email, long points) {
        long safePoints = Math.max(0, Math.min(points, 20_000));
        Client client = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung"))
                .getClient();
        client.addScore(safePoints);
        return client.getScore();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(user.getRole())
                .disabled(!user.isEnabled())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
