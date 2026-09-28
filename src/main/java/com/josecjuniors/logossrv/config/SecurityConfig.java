package com.josecjuniors.logossrv.config;

import com.josecjuniors.logossrv.config.jwt.JwtAuthenticationFilter;
import com.josecjuniors.logossrv.config.security.workload.WorkloadAuthenticationFailureResponder;
import com.josecjuniors.logossrv.config.security.workload.WorkloadAuthenticationProvider;
import com.josecjuniors.logossrv.config.security.workload.WorkloadBearerAuthenticationFilter;
import com.josecjuniors.logossrv.core.security.workload.application.WorkloadAuthenticationService;
import com.josecjuniors.logossrv.core.appuser.application.service.UserDetailsServiceImpl;
import com.josecjuniors.logossrv.core.appuser.domain.repository.AppUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.ProviderManager;
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
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
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
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // A chamada .authenticationProvider() foi removida, pois o Spring gerencia isso.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "logos.security.workload.http.enabled", havingValue = "true", matchIfMissing = false)
    public SecurityFilterChain workloadSecurityFilterChain(
            HttpSecurity http,
            WorkloadAuthenticationService workloadAuthenticationService) throws Exception {
        RequestMatcher workloadRequestMatcher = new AntPathRequestMatcher(
                "/api/internal/v1/progression/executions", "POST");
        AuthenticationManager workloadAuthenticationManager = new ProviderManager(
                new WorkloadAuthenticationProvider(workloadAuthenticationService));
        WorkloadBearerAuthenticationFilter workloadFilter = new WorkloadBearerAuthenticationFilter(
                workloadAuthenticationManager, workloadRequestMatcher, new WorkloadAuthenticationFailureResponder());

        http
                .securityMatcher(workloadRequestMatcher)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .addFilterBefore(workloadFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
