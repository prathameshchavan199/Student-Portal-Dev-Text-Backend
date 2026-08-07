package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.*;
import com.webapp.cognitodemo.repo.UserRepo;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.List;
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
        response.setProvider(user.getProvider());

        return response;
    }

    public AuthenticationResultType loginAndGetTokens(
            LoginRequest request) {

        return cognitoService.login(request);
    }

    // GOOGLE SIGN-IN — create the account on first login only
    public void signupGoogleUser(String email, String name) {

        cognitoService.signupGoogleUser(email, name);

        // A Postgres row can already exist here (e.g. left over from testing
        // against a different Cognito User Pool) — don't fail on that.
        if (!userExists(email)) {
            User user = new User();
            user.setFullName(name != null && !name.isBlank() ? name : email);
            user.setEmail(email);
            user.setProvider("GOOGLE");

            userRepo.save(user);
        }
    }

    // Whether the Cognito account exists in the *currently configured* pool
    public boolean cognitoUserExists(String email) {

        return cognitoService.cognitoUserExists(email);
    }

    public AuthenticationResultType loginGoogleUser(String email) {

        return cognitoService.loginGoogleUser(email);
    }

    public LoginResponse buildLoginResponse(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        LoginResponse response = new LoginResponse();

        response.setEmail(user.getEmail());
        response.setId(user.getId());
        response.setName(user.getFullName());
        response.setRegistered(user.isRegistration());
        response.setProvider(user.getProvider());

        return response;
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



