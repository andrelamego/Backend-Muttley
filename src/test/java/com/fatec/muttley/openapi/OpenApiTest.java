package com.fatec.muttley.openapi;

import com.fatec.muttley.config.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiTest {

    @Test
    @DisplayName("Deve configurar metadados e autenticacao JWT Bearer no OpenAPI")
    void deveConfigurarMetadadosESecurityScheme() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.muttleyOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Muttley API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getInfo().getDescription()).contains("Muttley");

        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");

        assertThat(openAPI.getSecurity()).isNotEmpty();
        assertThat(openAPI.getSecurity().get(0)).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);
    }
}
