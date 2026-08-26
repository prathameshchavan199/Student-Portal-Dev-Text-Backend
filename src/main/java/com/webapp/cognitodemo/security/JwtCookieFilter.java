package com.webapp.cognitodemo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.webapp.cognitodemo.repo.UserRepo;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final JwtDecoder googleJwtDecoder;
    private final UserRepo userRepo;

    public JwtCookieFilter(
            JwtDecoder jwtDecoder,
            @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
            UserRepo userRepo) {
        this.jwtDecoder = jwtDecoder;
        this.googleJwtDecoder = googleJwtDecoder;
        this.userRepo = userRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String requestPath = request.getRequestURI();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || requestPath.startsWith("/swagger-ui")
                || requestPath.startsWith("/v3/api-docs")
                || requestPath.equals("/api/users/login")
                || requestPath.equals("/api/users/refresh")
                || requestPath.equals("/api/users/signup")
                || requestPath.equals("/api/users/confirm")
                || requestPath.equals("/api/users/logout")
                || requestPath.equals("/api/users/verify-otp")
                || requestPath.equals("/api/users/forgot-password")
                || requestPath.equals("/api/users/reset-password")
                || requestPath.equals("/api/users/google-auth-url")
                || requestPath.equals("/api/users/google-callback");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        /*
         * Token resolution order:
         *   1. Authorization: Bearer <token> header  (Postman / API clients)
         *   2. idToken cookie  (ID token carries the email claim — preferred)
         *   3. accessToken cookie  (fallback; some Cognito pools add email to access token)
         */
        String accessToken = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        if (accessToken == null || accessToken.isBlank()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                String idTokenVal    = null;
                String accessTokenVal = null;
                for (Cookie cookie : cookies) {
                    if ("idToken".equals(cookie.getName()))     idTokenVal    = cookie.getValue();
                    if ("accessToken".equals(cookie.getName())) accessTokenVal = cookie.getValue();
                }
                // Prefer idToken because it always contains the email claim
                accessToken = (idTokenVal != null && !idTokenVal.isBlank())
                        ? idTokenVal : accessTokenVal;
            }
        }

        /*
         * TOKEN NOT PRESENT
         */
        if (accessToken == null || accessToken.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write(
                    "Authentication tokens are missing."
            );

            return;
        }

        try {

            /*
             * Try Cognito JWKS first; fall back to Google JWKS for users who
             * signed in via the direct Google OAuth flow.
             */
            String email = null;
            try {
                Jwt jwt = jwtDecoder.decode(accessToken);
                email = jwt.getClaimAsString("email");
            } catch (JwtException ignored) {
                Jwt jwt = googleJwtDecoder.decode(accessToken);
                email = jwt.getClaimAsString("email");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authoritiesFor(email)
                    );

            /*
             * Spring Security 6: create a new context and set it explicitly.
             * Using getContext().setAuthentication() risks modifying a stale
             * deferred-context instance that AuthorizationFilter won't see.
             */
            org.springframework.security.core.context.SecurityContext context =
                    SecurityContextHolder
                            .createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (JwtException ex) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write(
                    "Invalid or Expired Token: " + ex.getMessage()
            );

            return;

        } catch (Exception ex) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.getWriter().write(
                    "Authentication failed: " + ex.getMessage()
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    /*
     * Looks up the user's role in our own DB (not the Cognito token — the
     * token has no role claim) and returns the matching Spring Security
     * authority, e.g. "STUDENT" -> ROLE_STUDENT, "TPO_ADMIN" -> ROLE_TPO_ADMIN.
     * Defaults to ROLE_STUDENT if the user can't be found for any reason,
     * so a lookup failure never accidentally grants admin access.
     */
    private List<GrantedAuthority> authoritiesFor(String email) {
        String role = userRepo.findByEmail(email)
                .map(u -> u.getRole())
                .filter(r -> r != null && !r.isBlank())
                .orElse("STUDENT");
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
