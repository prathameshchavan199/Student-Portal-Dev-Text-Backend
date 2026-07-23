package com.webapp.cognitodemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Value("${AWS_AccessKey}")
    private String accessKey;

    @Value("${AWS_SecretKey}")
    private String secretKey;

    @Value("${AWS_Cognito_Region}")
    private String region;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {

        AwsBasicCredentials awsCredentials =
                AwsBasicCredentials.create(
                        accessKey,
                        secretKey
                );

        return CognitoIdentityProviderClient.builder()
                .region(
                        Region.of(region)
                )
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                awsCredentials
                        )
                )
                .build();
    }
}




//package com.webapp.cognitodemo.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
//
//@Configuration
//public class CognitoConfig {
//
//    @Bean
//    public CognitoIdentityProviderClient cognitoClient() {
//
//        return CognitoIdentityProviderClient.builder()
//                .region(Region.AP_SOUTH_1)
//                .build();
//    }
//}