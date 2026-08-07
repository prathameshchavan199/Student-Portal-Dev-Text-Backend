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

        // This pool's Username attribute is a generic username with email
        // set as an alias — signUp registers the email into that alias
        // lookup table so alias-based login (USERNAME=email) resolves it.
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

    /*
     * SYNTHETIC PASSWORD FOR GOOGLE-AUTHENTICATED USERS
     *
     * Cognito's ADMIN_USER_PASSWORD_AUTH flow always needs a password, but a
     * Google sign-in never provides one. Instead of storing a password
     * anywhere, we derive one deterministically from the email so it can be
     * recomputed identically on every future Google login for the same
     * account. It is never shown to or usable-knowledge of the user.
     */
    private String generateGooglePassword(String email) {

        try {

            String data = "google-oauth:" + email;

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256"));

            byte[] rawHmac = mac.doFinal(data.getBytes());

            String encoded = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawHmac);

            // Prefix guarantees upper/lower/digit/symbol regardless of what
            // the base64 alphabet happens to produce, satisfying Cognito's
            // default password policy.
            return "Gx1!" + encoded.substring(0, 20);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error while generating Google password",
                    e
            );
        }
    }

    /*
     * SIGNUP (GOOGLE)
     *
     * Same shape as signup(), but the password is the deterministic
     * synthetic one above instead of anything the user chose.
     */
    public void signupGoogleUser(String email, String name) {

        // This pool's Username attribute is a generic username with email
        // set as an alias — same shape as signup() above.
        String username = UUID.randomUUID().toString();

        SignUpRequest signUpRequest =
                SignUpRequest.builder()
                        .clientId(clientId)
                        .secretHash(calculateSecretHash(username))
                        .username(username)
                        .password(generateGooglePassword(email))
                        .userAttributes(

                                AttributeType.builder()
                                        .name("email")
                                        .value(email)
                                        .build(),

                                AttributeType.builder()
                                        .name("name")
                                        .value(name != null && !name.isBlank() ? name : email)
                                        .build()

                        )
                        .build();

        cognitoClient.signUp(signUpRequest);

        AdminConfirmSignUpRequest confirmRequest =
                AdminConfirmSignUpRequest.builder()
                        .userPoolId(userPoolID)
                        .username(username)
                        .build();

        cognitoClient.adminConfirmSignUp(confirmRequest);

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
    }

    /*
     * LOGIN (GOOGLE)
     *
     * Exchanges the same deterministic password for real Cognito tokens via
     * the existing ADMIN_USER_PASSWORD_AUTH flow.
     */
    public AuthenticationResultType loginGoogleUser(String email) {

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(generateGooglePassword(email));

        return login(request);
    }

    /*
     * Cognito is the actual system of record for account existence — our
     * Postgres table can go stale (e.g. after pointing the app at a
     * different User Pool during testing) and must not be trusted for this.
     */
    public boolean cognitoUserExists(String email) {

        try {
            getCognitoUsernameByEmail(email);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
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