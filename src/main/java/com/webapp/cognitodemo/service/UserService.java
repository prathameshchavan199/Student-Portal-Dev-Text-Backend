package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.*;
import com.webapp.cognitodemo.repo.UserRepo;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CognitoService cognitoService;

    // SIGNUP USER
    public String signupUser(com.webapp.cognitodemo.entity.@Valid SignupRequest request) {

        // STEP 1 -> Create in Cognito
        cognitoService.signup(request);

        // STEP 2 -> Save in PostgreSQL

        User user = new User();

        user.setFullName(request.getName());
        user.setEmail(request.getEmail());
        user.setProvider("LOCAL");

        userRepo.save(user);

        return "Signup successful. Verify OTP.";
    }

    // LOGIN
//    public AuthenticationResultType loginUser(
//            LoginRequest request) {
//
//        return cognitoService.login(request);
//    }

    public LoginResponse loginUser(
            LoginRequest request) {

        AuthenticationResultType authResult =
                cognitoService.login(request);

        User user = userRepo.findByEmail(
                request.getEmail()
        ).orElseThrow(() ->
                new RuntimeException(
                        "User not found"
                )
        );

        LoginResponse response =
                new LoginResponse();

        response.setEmail(user.getEmail());
        response.setId(user.getId());
        response.setName(user.getFullName());
        response.setRegistered(user.isRegistration());

        return response;
    }

    public AuthenticationResultType loginAndGetTokens(
            LoginRequest request) {

        return cognitoService.login(request);
    }
    
    // CONFIRM USER
    public String confirmUser(
            ConfirmRequest request) {

        cognitoService.confirmUser(request);

        return "User verified successfully";
    }

    // GET ALL USERS
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // GET USER BY ID
    public Optional<User> getUserById(Long id) {
        return userRepo.findById(id);
    }

    // DELETE USER
    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    public void logout(String token) {
        cognitoService.logout(token);
    }

    public User getUserByEmail(String email) {

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));
    }

    // GOOGLE LOGIN — exchange OAuth2 code directly with Google, find/create user
    public Map<String, Object> handleGoogleLogin(String code, String redirectUri) {

        Map<String, String> tokens = cognitoService.exchangeGoogleCodeForTokens(code, redirectUri);
        String idTokenStr = tokens.get("id_token");

        Map<String, Object> claims = cognitoService.verifyGoogleIdToken(idTokenStr);
        String email = (String) claims.get("email");
        String name  = (String) claims.get("name");
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        final String resolvedName = name;
        User user = userRepo.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setFullName(resolvedName);
            newUser.setEmail(email);
            newUser.setProvider("GOOGLE");
            return userRepo.save(newUser);
        });

        LoginResponse response = new LoginResponse();
        response.setEmail(user.getEmail());
        response.setId(user.getId());
        response.setName(user.getFullName());
        response.setRegistered(user.isRegistration());
        response.setIdToken(idTokenStr);
        response.setRefreshToken(tokens.get("refresh_token"));

        Map<String, Object> result = new HashMap<>();
        result.put("loginResponse", response);
        result.put("accessToken", tokens.get("access_token"));
        result.put("idToken", idTokenStr);
        result.put("refreshToken", tokens.get("refresh_token"));

        return result;
    }

    // CHECK IF USER EXISTS (used by forgot-password)
    public boolean userExists(String email) {

        return userRepo.findByEmail(email).isPresent();
    }

    // RESET PASSWORD
    public void resetPassword(
            String email,
            String newPassword) {

        // Make sure the account exists locally before touching Cognito
        userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));

        cognitoService.resetPassword(email, newPassword);
    }
}



