package com.webapp.cognitodemo.config;

import com.webapp.cognitodemo.security.JwtCookieFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.region}")
    private String region;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtCookieFilter jwtCookieFilter) throws Exception {

        http.cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(

                                "/api/users/signup",
                                "/api/users/login",
                                "/api/users/confirm",
                                "/api/users/logout",
                                "/api/users/verify-otp",
                                "/api/users/forgot-password",
                                "/api/users/reset-password",
                                "/api/users/refresh"

                        ).permitAll()
                        .requestMatchers("/api/registration/file/**").authenticated()
                        .anyRequest().authenticated()

                )

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request,
                                 response,
                                 authException) ->

                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )
                )

                .addFilterBefore(
                        jwtCookieFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                       "https://*.amplifyapp.com",
                        "https://master.d1bdgx8dlzpdhq.amplifyapp.com"

                )
        );
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("*")
        );
        configuration.setExposedHeaders(
                List.of("Set-Cookie")
        );
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            CorsConfigurationSource corsConfigurationSource) {

        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(
                        new CorsFilter(corsConfigurationSource)
                );

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }

    /*
     * Prevent Spring Boot from auto-registering JwtCookieFilter as a
     * servlet-level filter. It must only run inside the Spring Security
     * filter chain (added via addFilterBefore above). Without this,
     * OncePerRequestFilter's attribute mechanism causes the in-chain
     * execution to be skipped, leaving the SecurityContext empty.
     */
    @Bean
    public FilterRegistrationBean<JwtCookieFilter> jwtCookieFilterRegistration(
            JwtCookieFilter filter) {

        FilterRegistrationBean<JwtCookieFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);

        return registration;
    }


    @Bean
    public JwtDecoder jwtDecoder() {

        /*
         * Build the decoder from the known Cognito JWKS URI directly.
         * Unlike JwtDecoders.fromIssuerLocation(), this makes NO network
         * call at startup — keys are fetched lazily on the first token
         * validation, so a transient Cognito outage never prevents startup.
         */
        String jwksUri =
                "https://cognito-idp."
                        + region
                        + ".amazonaws.com/"
                        + userPoolId
                        + "/.well-known/jwks.json";

        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}
