package com.fatec.muttley.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI muttleyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Muttley API")
                        .description("API REST do Sistema Muttley para Gestão de Eventos, Participações, Emissão de Certificados e Medalhas da FATEC Zona Leste.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Equipe Muttley - FATEC Zona Leste")
                                .url("https://github.com/andrelamego/Backend-Muttley"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT gerado no endpoint /api/auth/login para autorizar as requisições aos recursos protegidos.")
                        )
                );
    }
}
