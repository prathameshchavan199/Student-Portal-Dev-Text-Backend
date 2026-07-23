package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.ConfirmRequest;
import com.webapp.cognitodemo.entity.LoginRequest;
import com.webapp.cognitodemo.entity.SignupRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CognitoService {

    @Autowired
    private CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.clientSecret}")
    private String clientSecret;

    @Value("${aws.cognito.userPoolId}")
    private String userPoolID;

    /*
     * GENERATE SECRET HASH
     */
    private String calculateSecretHash(
            String username) {

        try {

            String data = username + clientId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(
                            clientSecret.getBytes(),
                            "HmacSHA256"
                    );

            mac.init(secretKeySpec);

            byte[] rawHmac =
                    mac.doFinal(data.getBytes());

            return Base64.getEncoder()
                    .encodeToString(rawHmac);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error while calculating secret hash",
                    e
            );
        }
    }

    /*
     * SIGNUP
     *
     * Uses adminCreateUser with MessageAction.SUPPRESS so Cognito never
     * sends its own verification email. The app handles OTP via SMTP.
     * adminSetUserPassword with permanent=true moves the account out of
     * FORCE_CHANGE_PASSWORD and into CONFIRMED in one step.
     */
    public void signup(SignupRequest request) {

        String username = UUID.randomUUID().toString();
        System.out.println("[CognitoService.signup] username=" + username + " email=" + request.getEmail());

        // signUp (not adminCreateUser) is required to register the email into
        // Cognito's alias lookup table — adminCreateUser stores the email as
        // an attribute only and the alias-based login never resolves it.
        SignUpRequest signUpRequest =
                SignUpRequest.builder()
                        .clientId(clientId)
                        .secretHash(calculateSecretHash(username))
                        .username(username)
                        .password(request.getPassword())
                        .userAttributes(

                                AttributeType.builder()
                                        .name("email")
                                        .value(request.getEmail())
                                        .build(),

                                AttributeType.builder()
                                        .name("name")
                                        .value(request.getName())
                                        .build()

                        )
                        .build();

        cognitoClient.signUp(signUpRequest);
        System.out.println("[CognitoService.signup] signUp done");

        // Confirm immediately — email was verified by our own SMTP OTP
        AdminConfirmSignUpRequest confirmRequest =
                AdminConfirmSignUpRequest.builder()
                        .userPoolId(userPoolID)
                        .username(username)
                        .build();

        cognitoClient.adminConfirmSignUp(confirmRequest);
        System.out.println("[CognitoService.signup] adminConfirmSignUp done — user CONFIRMED");

        // Mark email as verified so the alias lookup works at login time
        AdminUpdateUserAttributesRequest verifyEmail =
                AdminUpdateUserAttributesRequest.builder()
                        .userPoolId(userPoolID)
                        .username(username)
                        .userAttributes(
                                AttributeType.builder()
                                        .name("email_verified")
                                        .value("true")
                                        .build()
                        )
                        .build();

        cognitoClient.adminUpdateUserAttributes(verifyEmail);
        System.out.println("[CognitoService.signup] email_verified=true set");
    }

    private String getCognitoUsernameByEmail(String email) {

        ListUsersRequest listRequest =
                ListUsersRequest.builder()
                        .userPoolId(userPoolID)
                        .filter("email = \"" + email + "\"")
                        .build();

        return cognitoClient.listUsers(listRequest)
                .users()
                .stream()
                .findFirst()
                .map(UserType::username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No Cognito user found for email: " + email
                        )
                );
    }




    /*
     * LOGIN
     *
     * Uses adminInitiateAuth with ADMIN_USER_PASSWORD_AUTH — the correct
     * server-side flow for a backend that already holds IAM credentials.
     * Requires ALLOW_ADMIN_USER_PASSWORD_AUTH enabled in the App Client.
     */
    public AuthenticationResultType login(
            LoginRequest request) {

        Map<String, String> authParams =
                new HashMap<>();

        authParams.put(
                "USERNAME",
                request.getEmail()
        );

        authParams.put(
                "PASSWORD",
                request.getPassword()
        );

        authParams.put(
                "SECRET_HASH",
                calculateSecretHash(
                        request.getEmail()
                )
        );

        AdminInitiateAuthRequest authRequest =
                AdminInitiateAuthRequest.builder()
                        .authFlow(
                                AuthFlowType.ADMIN_USER_PASSWORD_AUTH
                        )
                        .clientId(clientId)
                        .userPoolId(userPoolID)
                        .authParameters(authParams)
                        .build();

        AdminInitiateAuthResponse response =
                cognitoClient.adminInitiateAuth(authRequest);

        return response.authenticationResult();
    }

    /*
     * CONFIRM USER
     */
    public void confirmUser(
            ConfirmRequest request) {

        ConfirmSignUpRequest confirmRequest =
                ConfirmSignUpRequest.builder()
                        .clientId(clientId)
                        .secretHash(
                                calculateSecretHash(
                                        request.getEmail()
                                )
                        )
                        .username(request.getEmail())
                        .confirmationCode(request.getOtp())
                        .build();

        cognitoClient.confirmSignUp(confirmRequest);
    }

//    LOGOUT
    public void logout(String accessToken) {

        GlobalSignOutRequest request =
                GlobalSignOutRequest.builder()
                        .accessToken(accessToken)
                        .build();

        cognitoClient.globalSignOut(request);
    }

    /*
     * REFRESH TOKENS
     *
     * Exchanges a valid refresh token for a new accessToken + idToken.
     * The SECRET_HASH must use the real Cognito username (UUID sub),
     * not the email alias — so we look it up first.
     */
    public AuthenticationResultType refreshTokens(
            String refreshToken,
            String email) {

        String cognitoUsername = getCognitoUsernameByEmail(email);

        Map<String, String> params = new HashMap<>();
        params.put("REFRESH_TOKEN", refreshToken);
        params.put("SECRET_HASH", calculateSecretHash(cognitoUsername));

        AdminInitiateAuthRequest authRequest =
                AdminInitiateAuthRequest.builder()
                        .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                        .clientId(clientId)
                        .userPoolId(userPoolID)
                        .authParameters(params)
                        .build();

        return cognitoClient.adminInitiateAuth(authRequest)
                .authenticationResult();
    }

    /*
     * RESET PASSWORD (ADMIN)
     *
     * Sets a new permanent password for the user directly in Cognito.
     * This bypasses the old password and is only reached after the
     * caller has verified the OTP, so it must stay behind that check.
     */
    public void resetPassword(
            String email,
            String newPassword) {

        String username = getCognitoUsernameByEmail(email);

        AdminSetUserPasswordRequest request =
                AdminSetUserPasswordRequest.builder()
                        .userPoolId(userPoolID)
                        .username(username)
                        .password(newPassword)
                        .permanent(true)
                        .build();

        cognitoClient.adminSetUserPassword(request);
    }
}