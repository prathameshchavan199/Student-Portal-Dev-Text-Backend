package com.webapp.cognitodemo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/*
 * Verifies the Google ID token the frontend receives from Google Identity
 * Services. Verification is fully offline after the first call — the
 * verifier caches Google's public signing keys itself.
 */
@Service
public class GoogleAuthService {

    @Value("${google.clientId}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    private GoogleIdTokenVerifier verifier() {

        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }

        return verifier;
    }

    public GoogleIdToken.Payload verify(String idTokenString) {

        try {

            GoogleIdToken idToken = verifier().verify(idTokenString);

            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            return idToken.getPayload();

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Google token verification failed: " + e.getMessage(), e);
        }
    }
}
