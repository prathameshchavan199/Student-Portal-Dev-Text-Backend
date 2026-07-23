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



