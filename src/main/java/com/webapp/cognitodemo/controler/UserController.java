package com.webapp.cognitodemo.controler;

import com.webapp.cognitodemo.entity.*;
import com.webapp.cognitodemo.entity.OTP.OtpRequest;
import com.webapp.cognitodemo.entity.OTP.VerifyOtpRequest;
import com.webapp.cognitodemo.service.CognitoService;
import com.webapp.cognitodemo.service.EmailService;
import com.webapp.cognitodemo.service.OtpService;
import com.webapp.cognitodemo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//import sendinblue.ApiException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Users", description = "Authentication, signup, OTP, password reset and user management")
@RestController
@RequestMapping("/api/users")

public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private CognitoService cognitoService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private EmailService emailService;

    /*
     * SIGNUP — step 1: send OTP only, nothing written to Cognito or DB yet.
     * Registration happens in /verify-otp once the code is confirmed.
     */
    @Operation(summary = "Sign up — sends OTP to email; call /verify-otp to complete")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequest request) {

        if (userService.userExists(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "An account with this email already exists"
                    ));
        }

        otpService.storePendingSignup(
                request.getEmail(),
                request
        );

        String otp = otpService.generateOtp(request.getEmail());

        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to send OTP email"
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent to your email. Please verify to complete signup."
        ));
    }

    /*
     * CONFIRM USER
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmUser(
            @RequestBody ConfirmRequest request) {

        return ResponseEntity.ok(
                Map.of(
                        "message", "OTP sent successfully",
                        "data", userService.confirmUser(request)
                )
        );
    }

    /*
     * LOGIN
     */
    @Operation(summary = "Login — returns user info and sets accessToken/idToken/refreshToken cookies")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        AuthenticationResultType authResult =
                userService.loginAndGetTokens(
                        request
                );

        LoginResponse response =
                userService.loginUser(
                        request
                );

        response.setIdToken(authResult.idToken());
        response.setRefreshToken(authResult.refreshToken());

        ResponseCookie accessTokenCookie =
                ResponseCookie.from(
                                "accessToken",
                                authResult.accessToken()
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(3600)
                        .build();

        ResponseCookie idTokenCookie =
                ResponseCookie.from(
                                "idToken",
                                authResult.idToken()
                        )
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(3600)
                        .build();

        ResponseCookie refreshTokenCookie =
                ResponseCookie.from(
                                "refreshToken",
                                authResult.refreshToken()
                        )
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("none")
                        .path("/")
                        .maxAge(86400)
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, idTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(response);
    }

    /*
     * REFRESH TOKENS
     *
     * Called automatically by the frontend axios interceptor when a 401 is
     * received. Reads the refreshToken HttpOnly cookie, exchanges it with
     * Cognito for a fresh accessToken + idToken, and sets new cookies.
     * The email is needed only to look up the Cognito username for the
     * SECRET_HASH calculation.
     */
    @Operation(summary = "Refresh tokens — accepts refreshToken from cookie or request body; returns new idToken in body")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokens(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            @RequestBody Map<String, String> body) {

        String refreshToken = (refreshTokenCookie != null && !refreshTokenCookie.isBlank())
                ? refreshTokenCookie
                : body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "No refresh token"));
        }

        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email is required"));
        }

        try {
            AuthenticationResultType authResult =
                    cognitoService.refreshTokens(refreshToken, email);

            ResponseCookie accessTokenCookie =
                    ResponseCookie.from("accessToken", authResult.accessToken())
                            .httpOnly(true).secure(false).sameSite("Lax")
                            .path("/").maxAge(3600).build();

            ResponseCookie idTokenCookie =
                    ResponseCookie.from("idToken", authResult.idToken())
                            .httpOnly(true).secure(false).sameSite("Lax")
                            .path("/").maxAge(3600).build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, idTokenCookie.toString())
                    .body(Map.of("success", true, "idToken", authResult.idToken()));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Token refresh failed — please log in again"));
        }
    }

    /*
     * CURRENT USER
     */

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication) {

        String email =
                authentication.getName();

        User user =
                userService.getUserByEmail(
                        email
                );

        return ResponseEntity.ok(user);
    }


@Operation(summary = "Verify OTP — completes signup or validates OTP before password reset")
@PostMapping("/verify-otp")
public ResponseEntity<?> verifyOtp(
        @RequestBody VerifyOtpRequest request) {

    System.out.println("[verify-otp] Request received for: " + request.getEmail());

    boolean valid =
            otpService.verifyOtp(
                    request.getEmail(),
                    request.getOtp()
            );

    System.out.println("[verify-otp] OTP valid: " + valid);

    if (!valid) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "success", false,
                        "message", "Invalid or Expired OTP"
                ));
    }

    // --- SIGNUP FLOW ---
    SignupRequest pendingSignup =
            otpService.getPendingSignup(request.getEmail());

    System.out.println("[verify-otp] Pending signup found: " + (pendingSignup != null));

    if (pendingSignup != null) {
        try {
            System.out.println("[verify-otp] Calling signupUser for: " + request.getEmail());
            userService.signupUser(pendingSignup);
            System.out.println("[verify-otp] signupUser completed successfully");
        } catch (Exception e) {
            System.out.println("[verify-otp] signupUser FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                    ));
        }

        otpService.removePendingSignup(request.getEmail());
        otpService.removeOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Account created successfully. You can now log in."
        ));
    }

    // --- FORGOT-PASSWORD FLOW ---
    // OTP intentionally kept; /reset-password will verify it again before
    // changing the password, then call removeOtp itself.
    return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "OTP verified successfully"
    ));
}

    /*
     * FORGOT PASSWORD -> SEND OTP
     *
     * Frontend sends the email. If the account exists we generate an
     * OTP and email it, then the frontend shows the OTP popup.
     */
    @Operation(summary = "Forgot password — sends OTP to the registered email")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        if (!userService.userExists(request.getEmail())) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "No account found with this email"
                            )
                    );
        }

        try {

            String otp =
                    otpService.generateOtp(
                            request.getEmail()
                    );

            emailService.sendOtpEmail(
                    request.getEmail(),
                    otp
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Failed to send OTP email"
                            )
                    );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "OTP sent to your email"
                )
        );
    }

    /*
     * RESET PASSWORD -> VERIFY OTP + SET NEW PASSWORD
     *
     * Called from the new-password page. The OTP is verified again
     * here (server-side) before the password is changed in Cognito,
     * so this endpoint is safe even if called directly.
     */
    @Operation(summary = "Reset password — verifies OTP again then sets new password in Cognito")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        // 1. Verify the OTP
        boolean valid =
                otpService.verifyOtp(
                        request.getEmail(),
                        request.getOtp()
                );

        if (!valid) {

            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Invalid or Expired OTP"
                            )
                    );
        }

        // 2. Update the password in Cognito
        try {

            userService.resetPassword(
                    request.getEmail(),
                    request.getNewPassword()
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            Map.of(
                                    "success", false,
                                    "message",
                                    "Failed to reset password"
                            )
                    );
        }

        // 3. Invalidate the used OTP
        otpService.removeOtp(
                request.getEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Password reset successfully"
                )
        );
    }

    /*
     * GET ALL USERS
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    /*
     * GET USER BY ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id) {

        Optional<User> user =
                userService.getUserById(id);

        return user
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    /*
     * DELETE USER
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return ResponseEntity.ok(
                "User deleted successfully"
        );
    }

    /*
     * LOGOUT
     */
    @Operation(summary = "Logout — clears accessToken and refreshToken cookies")
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {

        ResponseCookie accessTokenCookie =
                ResponseCookie.from(
                                "accessToken",
                                ""
                        )
                        .httpOnly(true)
                        .path("/")
                        .maxAge(0)
                        .build();

        ResponseCookie refreshTokenCookie =
                ResponseCookie.from(
                                "refreshToken",
                                ""
                        )
                        .httpOnly(true)
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessTokenCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookie.toString()
                )
                .body(
                        "Logged out successfully"
                );
    }
}


