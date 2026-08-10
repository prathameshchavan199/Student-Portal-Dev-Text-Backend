package com.webapp.cognitodemo.service;

import com.webapp.cognitodemo.entity.ConfirmRequest;
import com.webapp.cognitodemo.entity.LoginRequest;
import com.webapp.cognitodemo.entity.SignupRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Value("${aws.cognito.domain}")
    private String cognitoDomain;

    @Value("${google.oauth2.client-id}")
    private String googleClientId;

    @Value("${google.oauth2.client-secret}")
    private String googleClientSecret;

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

    /*
     * GOOGLE OAUTH2 — exchange authorization code for tokens
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> exchangeCodeForTokens(String code, String redirectUri) {

        String tokenEndpoint = cognitoDomain + "/oauth2/token";

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Cognito token endpoint");
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", (String) responseBody.get("access_token"));
        tokens.put("id_token",     (String) responseBody.get("id_token"));
        tokens.put("refresh_token", (String) responseBody.getOrDefault("refresh_token", ""));

        return tokens;
    }

    /*
     * Build the Cognito Hosted UI URL for Google federated sign-in
     */
    public String getGoogleAuthUrl(String redirectUri) {

        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return cognitoDomain + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + encodedRedirectUri
                + "&identity_provider=Google"
                + "&scope=email+openid+profile";
    }

    /*
     * DIRECT GOOGLE OAUTH2 — exchange authorization code with Google's token endpoint
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> exchangeGoogleCodeForTokens(String code, String redirectUri) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) throw new RuntimeException("Empty response from Google token endpoint");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", (String) responseBody.get("access_token"));
        tokens.put("id_token",     (String) responseBody.get("id_token"));
        tokens.put("refresh_token", (String) responseBody.getOrDefault("refresh_token", ""));
        return tokens;
    }

    /*
     * Verify Google id_token via Google's tokeninfo endpoint and return its claims.
     * Also validates that the token's audience matches our Google client ID.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyGoogleIdToken(String idToken) {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken, Map.class);

        Map<String, Object> claims = response.getBody();
        if (claims == null) throw new RuntimeException("Could not verify Google id_token");

        String aud = (String) claims.get("aud");
        if (!googleClientId.equals(aud)) throw new RuntimeException("Google token audience mismatch");

        return claims;
    }

    /*
     * DIRECT GOOGLE REFRESH — exchange Google refresh_token for new access + id tokens
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> refreshGoogleTokens(String refreshToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token", requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) throw new RuntimeException("Empty response from Google refresh endpoint");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", (String) responseBody.get("access_token"));
        tokens.put("id_token",     (String) responseBody.get("id_token"));
        return tokens;
    }
}