package com.flashcardapp.config;

import com.flashcardapp.helper.exception.CustomAccessDeniedHandler;
import com.flashcardapp.helper.exception.CustomAuthenticationEntryPoint;
import com.flashcardapp.helper.path.AppPath;
import com.flashcardapp.helper.path.SecurityPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationEntryPoint authenticationEntryPoint,
                                                   CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .invalidSessionUrl(AppPath.LOGIN + "?expired")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl(AppPath.LOGIN + "?expired")
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                        "font-src 'self' https://cdn.jsdelivr.net data:; " +
                                        "img-src 'self' data:; " +
                                        "connect-src 'self'; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self'; " +
                                        "form-action 'self'; " +
                                        "frame-ancestors 'none'"
                        ))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()"))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SecurityPath.PUBLIC_PATHS).permitAll()
                        .requestMatchers(SecurityPath.ADMIN_PATHS).hasRole("ADMIN")
                        .requestMatchers(SecurityPath.API_PATHS).authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage(AppPath.LOGIN)
                        .loginProcessingUrl(AppPath.LOGIN)
                        .defaultSuccessUrl(AppPath.LIBRARY, true)
                        .failureUrl(AppPath.LOGIN + "?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl(AppPath.LOGIN + "?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
