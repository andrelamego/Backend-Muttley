package com.fatec.muttley.certificado;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Monta o HTML do certificado a partir do modelo da coordenação. O mesmo HTML vira PDF e serve de prévia,
 * então o que a coordenação vê na tela é o que o participante recebe.
 */
@Component
public class CertificadoHtml {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter POR_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM", PT_BR);
    private static final String TINTA = "#0e1330";

    /** Dados de um certificado concreto (ou de exemplo, na prévia). */
    public record Dados(
            String nome,
            String tipo,
            String evento,
            LocalDate dataEvento,
            String carga,
            LocalDate dataEmissao,
            String codigo,
            String urlValidacao,
            String assinaturaBase64,
            String assinaturaMime) {
    }

    private final TemplateEngine templateEngine;
    private final String fonteTitulo;
    private final String fonteTexto;

    public CertificadoHtml(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        // O Chromium do serviço de PDF não tem as fontes da identidade; elas vão embutidas no HTML.
        this.fonteTitulo = base64("certificado/fontes/bricolage-grotesque.woff2");
        this.fonteTexto = base64("certificado/fontes/schibsted-grotesk.woff2");
    }

    public Map<String, Object> variaveis(ModeloCertificado modelo, Dados dados) {
        Map<String, Object> v = new HashMap<>();
        v.put("modelo", modelo);
        v.put("nome", dados.nome());
        v.put("corpo", corpo(modelo.getTexto(), marcadores(dados)));
        v.put("dia", dados.dataEvento() != null ? String.valueOf(dados.dataEvento().getDayOfMonth()) : "");
        v.put("mes", dados.dataEvento() != null ? MES.format(dados.dataEvento()).replace(".", "").toUpperCase(PT_BR) : "");
        v.put("ano", dados.dataEvento() != null ? String.valueOf(dados.dataEvento().getYear()) : "");
        v.put("dataEmissao", dados.dataEmissao() != null ? CURTA.format(dados.dataEmissao()) : "");
        v.put("codigo", dados.codigo());
        // O código já vem impresso ao lado; aqui basta o endereço da consulta, sem protocolo.
        v.put("url", enderecoDeConsulta(dados.urlValidacao(), dados.codigo()));
        v.put("assinaturaBase64", dados.assinaturaBase64() == null ? "" : dados.assinaturaBase64());
        v.put("assinaturaMime", dados.assinaturaMime() == null ? "image/png" : dados.assinaturaMime());
        v.put("logoSrc", modelo.getLogoBase64() == null ? null
                : "data:" + modelo.getLogoMime() + ";base64," + modelo.getLogoBase64());
        // As cores entram cruas no CSS: só passam no formato #rrggbb, senão vale a cor padrão.
        String principal = corSegura(modelo.getCorPrincipal(), "#2236e0");
        String destaque = corSegura(modelo.getCorDestaque(), "#ffd93b");
        v.put("corPrincipal", principal);
        v.put("corDestaque", destaque);
        v.put("textoSobrePrincipal", corDoTextoSobre(principal));
        v.put("textoSobreDestaque", corDoTextoSobre(destaque));
        v.put("fonteTitulo", fonteTitulo);
        v.put("fonteTexto", fonteTexto);
        return v;
    }

    public String renderizar(Map<String, Object> variaveis) {
        Context context = new Context(PT_BR);
        context.setVariables(variaveis);
        return templateEngine.process("public/certificados/modeloPdf", context);
    }

    private static Map<String, String> marcadores(Dados d) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("{nome}", d.nome());
        m.put("{tipo}", d.tipo() == null ? "participante" : d.tipo().toLowerCase(PT_BR));
        m.put("{evento}", d.evento());
        m.put("{data}", d.dataEvento() != null ? POR_EXTENSO.format(d.dataEvento()) : "data não informada");
        m.put("{carga}", d.carga());
        return m;
    }

    /**
     * Escapa o texto da coordenação e só então troca os marcadores pelos valores (também escapados) em negrito.
     * Assim nada que a coordenação ou o participante digitar vira HTML.
     */
    static String corpo(String texto, Map<String, String> valores) {
        String html = HtmlUtils.htmlEscape(texto == null ? "" : texto);
        for (var e : valores.entrySet()) {
            String valor = e.getValue() == null ? "" : e.getValue();
            html = html.replace(e.getKey(), "<strong>" + HtmlUtils.htmlEscape(valor) + "</strong>");
        }
        return html;
    }

    /** Branco ou tinta, o que der mais contraste sobre a cor (WCAG, luminância relativa). */
    static String corDoTextoSobre(String hex) {
        double fundo = luminancia(hex);
        double contrasteBranco = 1.05 / (fundo + 0.05);
        double contrasteTinta = (fundo + 0.05) / (luminancia(TINTA) + 0.05);
        return contrasteBranco >= contrasteTinta ? "#ffffff" : TINTA;
    }

    static String enderecoDeConsulta(String url, String codigo) {
        if (url == null) return null;
        String curta = url.replaceFirst("^https?://", "");
        return codigo != null && curta.endsWith("/" + codigo) ? curta.substring(0, curta.length() - codigo.length() - 1) : curta;
    }

    static String corSegura(String cor, String padrao) {
        return cor != null && cor.matches("^#[0-9a-fA-F]{6}$") ? cor : padrao;
    }

    private static double luminancia(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double[] c = {(rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff};
        for (int i = 0; i < 3; i++) {
            double s = c[i] / 255;
            c[i] = s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    private static String base64(String caminho) {
        try (InputStream in = new ClassPathResource(caminho).getInputStream()) {
            return Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Fonte do certificado ausente: " + caminho, e);
        }
    }
}
