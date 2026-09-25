package com.fatec.muttley.certificado;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.participacao.Participacao;
import com.fatec.muttley.pdf.PdfClient;
import com.fatec.muttley.pessoa.Pessoa;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Tag(name = "Certificados Públicos", description = "Validação, consulta pública, visualização e download de certificados emitidos")
@Controller
public class CertificadoPublicoController {
    private static final Logger log = LoggerFactory.getLogger(CertificadoPublicoController.class);

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private PdfClient pdfClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Operation(summary = "Consultar dados públicos do certificado",
            description = "Retorna os detalhes do certificado pelo código de validação, incluindo o link pronto para adição ao perfil do LinkedIn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificado encontrado"),
            @ApiResponse(responseCode = "404", description = "Certificado não encontrado")
    })
    @GetMapping("/api/certificados/{codigo}")
    public ResponseEntity<Map<String, Object>> dadosCertificadoPublico(
            @Parameter(description = "Código de validação do certificado", example = "ABC123XYZ")
            @PathVariable String codigo,
            HttpServletRequest request) {
        Certificado certificado = buscarCertificado(codigo);
        return ResponseEntity.ok(Map.of(
                "certificado", certificado,
                "linkedinUrl", montarUrlLinkedIn(certificado, request)
        ));
    }

    @Operation(summary = "Pré-visualizar PDF do certificado",
            description = "Gera e exibe o PDF do certificado inline no navegador através do código de validação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Certificado não encontrado")
    })
    @GetMapping("/api/certificados/{codigo}/preview")
    public ResponseEntity<byte[]> preview(
            @Parameter(description = "Código de validação do certificado")
            @PathVariable String codigo, Model model) throws IOException {
        Certificado certificado = buscarCertificado(codigo);
        preencherModelo(certificado, model);
        return gerarPdf(model, "inline");
    }

    @Operation(summary = "Download do PDF do certificado",
            description = "Gera o PDF do certificado e força o download (attachment) pelo código de validação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Download iniciado"),
            @ApiResponse(responseCode = "404", description = "Certificado não encontrado")
    })
    @GetMapping("/api/certificados/{codigo}/download")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "Código de validação do certificado")
            @PathVariable String codigo, Model model) throws IOException {
        Certificado certificado = buscarCertificado(codigo);
        preencherModelo(certificado, model);
        return gerarPdf(model, "attachment");
    }

    private Certificado buscarCertificado(String codigo) {
        return certificadoService.procurarPorCodigoValidacao(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado."));
    }

    private String montarUrlLinkedIn(Certificado certificado, HttpServletRequest request) {
        Participacao participacao = certificado.getParticipacao();
        Evento evento = participacao != null ? participacao.getEvento() : null;
        LocalDate dataEmissao = certificado.getDataEmissao() != null ? certificado.getDataEmissao() : evento != null ? evento.getData() : null;
        String codigo = certificado.getCodigoValidacao();
        String urlCertificado = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/certificados/" + codigo)
                .replaceQuery(null)
                .build()
                .toUriString();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://www.linkedin.com/profile/add")
                .queryParam("startTask", "CERTIFICATION_NAME")
                .queryParam("name", montarNomeCertificado(evento))
                .queryParam("organizationName", "FATEC Zona Leste")
                .queryParam("certId", codigo)
                .queryParam("certUrl", urlCertificado);

        if (dataEmissao != null) {
            builder.queryParam("issueYear", dataEmissao.getYear());
            builder.queryParam("issueMonth", dataEmissao.getMonthValue());
        }

        return builder.build().encode().toUriString();
    }

    private ResponseEntity<byte[]> gerarPdf(Model model, String disposition) throws IOException {
        Context context = new Context();
        context.setVariables(model.asMap());
        String htmlProcessado = templateEngine.process("public/certificados/modeloPdf", context);

        byte[] pdfBytes = pdfClient.gerarPdf(htmlProcessado);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"certificado.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    private String montarNomeCertificado(Evento evento) {
        if (evento == null || evento.getTema() == null || evento.getTema().isBlank()) {
            return "Certificado Muttley";
        }
        return "Certificado - " + evento.getTema();
    }

    private Map<String, Object> preencherModelo(Certificado certificado) {
        Participacao participacao = certificado.getParticipacao();
        Evento evento = participacao != null ? participacao.getEvento() : null;
        Pessoa pessoa = participacao != null ? participacao.getPessoa() : null;

        String assinaturaBase64 = "";
        String assinaturaMime = "image/png";
        if (certificado.getCaminhoAssinaturaVisual() != null && !certificado.getCaminhoAssinaturaVisual().isBlank()) {
            try {
                Path path = Path.of(certificado.getCaminhoAssinaturaVisual());
                byte[] imageBytes = Files.readAllBytes(path);
                assinaturaBase64 = Base64.getEncoder().encodeToString(imageBytes);
                assinaturaMime = AssinaturaStorage.mime(path);
            } catch (IOException | InvalidPathException exception) {
                log.warn("Não foi possível carregar a assinatura visual do certificado {}: {}", certificado.getId(), exception.getMessage());
            }
        }

        return Map.of(
                "certificado", certificado,
                "assinaturaBase64", assinaturaBase64,
                "assinaturaMime", assinaturaMime,
                "pessoa", participacao != null && participacao.getTipo() != null ? participacao.getTipo() : "participante",
                "nome", pessoa != null ? pessoa.getNome() : "Participante",
                "preambulo", "Por participar do evento ",
                "evento", evento != null ? evento.getTema() : "Evento",
                "predicado", montarPredicado(evento),
                "duracao", calcularDuracao(evento),
                "data", formatarDataEmissao(certificado.getDataEmissao())
        );
    }

    private void preencherModelo(Certificado certificado, Model model) {
        preencherModelo(certificado).forEach(model::addAttribute);
    }

    private String montarPredicado(Evento evento) {
        if (evento == null || evento.getData() == null) {
            return "promovido pela FATEC Zona Leste.";
        }
        return "realizado no dia " + evento.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ", promovido pela FATEC Zona Leste.";
    }

    private String calcularDuracao(Evento evento) {
        if (evento == null || evento.getHorarioInicio() == null || evento.getHorarioFim() == null) {
            return "carga horária não informada.";
        }

        try {
            LocalTime inicio = LocalTime.parse(evento.getHorarioInicio());
            LocalTime fim = LocalTime.parse(evento.getHorarioFim());
            long minutos = Duration.between(inicio, fim).toMinutes();
            if (minutos <= 0) {
                return "carga horária não informada.";
            }

            long horas = minutos / 60;
            long minutosRestantes = minutos % 60;
            if (minutosRestantes == 0) {
                return horas + (horas == 1 ? " hora." : " horas.");
            }
            return horas + "h" + String.format("%02d", minutosRestantes) + ".";
        } catch (DateTimeParseException exception) {
            return "carga horária não informada.";
        }
    }

    private String formatarDataEmissao(LocalDate dataEmissao) {
        if (dataEmissao == null) {
            return "São Paulo";
        }
        return "São Paulo, " + dataEmissao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Operation(summary = "Exibir imagem da assinatura pública",
            description = "Retorna a imagem da assinatura do certificado a partir do identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem retornada"),
            @ApiResponse(responseCode = "404", description = "Assinatura ou certificado não encontrado")
    })
    @GetMapping("/{id}/assinatura-visual")
    public ResponseEntity<Resource> exibirImagemPublica(@PathVariable Long id) {
        return certificadoService.procurarPorId(id)
                .map(certificado -> AssinaturaVisualResponse.carregar(certificado.getCaminhoAssinaturaVisual()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
