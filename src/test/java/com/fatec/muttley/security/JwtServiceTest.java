package com.fatec.muttley.security;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.util.ReflectionTestUtils;

import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    // Chave exclusivamente sintética, nunca usada pela aplicação.
    private SecurityConfig config(String secret) {
        SecurityConfig config=new SecurityConfig(); ReflectionTestUtils.setField(config,"jwtSecret",secret); return config;
    }
    @Test void RN_AUT_07_08_tokenAssinadoContemIdentidadePerfilEDuasHorasDeValidade() {
        SecurityConfig config=config("chave-exclusiva-de-teste-com-mais-de-32-bytes");
        JwtService service=new JwtService(config.jwtEncoder(),Duration.ofHours(2));
        Instant antes=Instant.now().minusSeconds(1);
        Jwt jwt=config.jwtDecoder().decode(service.gerarToken(pessoa(7)));
        assertThat(jwt.getSubject()).isEqualTo("pessoa7@example.invalid");
        assertThat(((Number)jwt.getClaim("userId")).longValue()).isEqualTo(7);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getIssuedAt()).isBetween(antes,Instant.now());
        assertThat(Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt())).isEqualTo(Duration.ofHours(2));
        assertThat(service.getExpirationSeconds()).isEqualTo(7200);
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
    }
    @Test void tokenComAssinaturaDeOutraChaveERejeitado() {
        SecurityConfig config=config("chave-exclusiva-de-teste-com-mais-de-32-bytes");
        String token=new JwtService(config.jwtEncoder(),Duration.ofHours(2)).gerarToken(pessoa(7));
        JwtDecoder decoder=config("outra-chave-exclusiva-de-teste-com-32-bytes").jwtDecoder();
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
    @Test void tokenExpiradoERejeitado() {
        SecurityConfig config=config("chave-exclusiva-de-teste-com-mais-de-32-bytes");
        String token=config.jwtEncoder().encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject("teste@example.invalid").issuedAt(Instant.now().minusSeconds(7200))
                        .expiresAt(Instant.now().minusSeconds(3600)).build())).getTokenValue();
        assertThatThrownBy(() -> config.jwtDecoder().decode(token)).isInstanceOf(JwtValidationException.class);
    }
}
