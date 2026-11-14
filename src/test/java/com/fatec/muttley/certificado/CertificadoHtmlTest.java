package com.fatec.muttley.certificado;

import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificadoHtmlTest {
    private static CertificadoHtml renderizador() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return new CertificadoHtml(engine);
    }

    private static CertificadoHtml.Dados dados(String nome) {
        return new CertificadoHtml.Dados(nome, "Palestrante", "Semana <b>Tech</b>", LocalDate.of(2026, 11, 12),
                "3 horas", LocalDate.of(2026, 11, 13), "codigo-123", "https://muttley.example.invalid/certificados/codigo-123",
                null, null);
    }

    @Test void textoDaCoordenacaoEValoresSaoEscapadosAntesDosMarcadores() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("{nome}", "<script>x</script>");
        String corpo = CertificadoHtml.corpo("<i>Oi</i> {nome}", valores);
        assertThat(corpo).isEqualTo("&lt;i&gt;Oi&lt;/i&gt; <strong>&lt;script&gt;x&lt;/script&gt;</strong>");
    }

    @Test void escolheTextoBrancoOuTintaPeloContraste() {
        assertThat(CertificadoHtml.corDoTextoSobre("#2236e0")).isEqualTo("#ffffff");
        assertThat(CertificadoHtml.corDoTextoSobre("#ffd93b")).isEqualTo("#0e1330");
    }

    @Test void enderecoDeConsultaSemProtocoloNemCodigo() {
        assertThat(CertificadoHtml.enderecoDeConsulta("https://m.example/certificados/abc", "abc")).isEqualTo("m.example/certificados");
        assertThat(CertificadoHtml.enderecoDeConsulta(null, "abc")).isNull();
    }

    @Test void corForaDoFormatoNaoEntraNoCss() {
        assertThat(CertificadoHtml.corSegura("red;} body{display:none", "#2236e0")).isEqualTo("#2236e0");
        assertThat(CertificadoHtml.corSegura("#AbCdEf", "#2236e0")).isEqualTo("#AbCdEf");
    }

    @Test void rendereizaOsDoisLeiautesComOsDadosEOModelo() {
        var html = renderizador();
        ModeloCertificado modelo = ModeloCertificado.padrao();
        modelo.setTexto("Participou como {tipo} de {evento} em {data} ({carga}).");
        String ingresso = html.renderizar(html.variaveis(modelo, dados("Ana Souza")));
        assertThat(ingresso).contains("class=\"folha ingresso\"", "Ana Souza", "<strong>palestrante</strong>",
                "<strong>Semana &lt;b&gt;Tech&lt;/b&gt;</strong>", "12 de novembro de 2026", "NOV 2026",
                "codigo-123", "Emitido em 13/11/2026", "@page", "font/woff2;base64,");

        modelo.setLeiaute(LeiauteCertificado.CLASSICO);
        modelo.setCorPrincipal("#7a1f2b");
        String classico = html.renderizar(html.variaveis(modelo, dados("Bruno")));
        assertThat(classico).contains("class=\"folha classico\"", "#7a1f2b").doesNotContain("class=\"folha ingresso\"");
    }

    @Test void logoPrecisaSerImagemPngOuJpgDeVerdade() {
        String png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        assertThat(ModeloCertificadoService.mimeDaLogo(png)).isEqualTo("image/png");
        String texto = Base64.getEncoder().encodeToString("<svg onload=alert(1)>".getBytes());
        assertThatThrownBy(() -> ModeloCertificadoService.mimeDaLogo(texto)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModeloCertificadoService.mimeDaLogo("não é base64")).isInstanceOf(IllegalArgumentException.class);
    }
}
