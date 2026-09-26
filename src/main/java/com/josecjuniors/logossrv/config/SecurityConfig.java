package com.josecjuniors.logossrv.config;

import com.josecjuniors.logossrv.config.jwt.JwtAuthenticationFilter;
import com.josecjuniors.logossrv.config.progression.ProgressionIntegrationClientProperties;
import com.josecjuniors.logossrv.config.progression.ProgressionSubjectLinkProperties;
import com.josecjuniors.logossrv.adapters.in.web.progression.security.ProgressionIntegrationAuthenticationFilter;
import com.josecjuniors.logossrv.adapters.in.web.progression.security.ProgressionIntegrationPrincipal;
import com.josecjuniors.logossrv.adapters.in.web.progression.security.ProgressionIntegrationRequestMatchers;
import com.josecjuniors.logossrv.core.appuser.application.service.UserDetailsServiceImpl;
import com.josecjuniors.logossrv.core.appuser.domain.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ProgressionIntegrationClientProperties.class, ProgressionSubjectLinkProperties.class})
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository appUserRepository) {
        return new UserDetailsServiceImpl(appUserRepository);
    }

    // O bean de AuthenticationProvider foi removido. O Spring Boot irá configurar
    // um DaoAuthenticationProvider automaticamente, pois ele encontra os beans
    // de UserDetailsService e PasswordEncoder.

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter,
                                                   ProgressionIntegrationAuthenticationFilter integrationAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(ProgressionIntegrationRequestMatchers.protectedEndpoints())
                        .hasAuthority(ProgressionIntegrationPrincipal.AUTHORITY)
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, ignored) -> {
                    if (ProgressionIntegrationRequestMatchers.isProtected(request)) {
                        response.sendError(401);
                    } else {
                        response.sendError(403);
                    }
                }))
                // A chamada .authenticationProvider() foi removida, pois o Spring gerencia isso.
                .addFilterBefore(integrationAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, ProgressionIntegrationAuthenticationFilter.class);

        return http.build();
    }
}
