package com.webapp.cognitodemo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Portal API")
                        .description("""
                                REST API for the Student Portal backend.

                                **Authentication:** After calling `/api/users/login`, copy the `idToken` \
                                value from the browser cookie and paste it into the Authorize dialog \
                                (Bearer token). All secured endpoints require this token.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Student Portal Team")
                                .email("cprathamesh199@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Ec2 Server development")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your Cognito idToken here (copy from browser DevTools → Application → Cookies after login)")));
    }
}
