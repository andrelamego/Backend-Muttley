package com.fatec.muttley.certificado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.pdf.PdfClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.thymeleaf.TemplateEngine;

class CertificadoAssinaturaVisualTest {
    @TempDir Path diretorio;

    private final CertificadoService certificados = mock(CertificadoService.class);
    private final CertificadoController admin = new CertificadoController(
            certificados, mock(EventoService.class), mock(AssinaturaStorage.class));
    private final CertificadoPublicoController publico = new CertificadoPublicoController(
            certificados, mock(PdfClient.class), mock(TemplateEngine.class));

    @Test
    void certificadoInexistenteRetorna404NasDuasRotas() {
        assertThat(admin.exibirImagemDaPasta(7L).getStatusCode().value()).isEqualTo(404);
        assertThat(publico.exibirImagemPublica(7L).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void assinaturaAusenteOuCaminhoInvalidoRetorna404() {
        Certificado certificado = new Certificado();
        when(certificados.procurarPorId(7L)).thenReturn(Optional.of(certificado));

        assertNotFound();
        certificado.setCaminhoAssinaturaVisual(diretorio.resolve("nao-existe.png").toString());
        assertNotFound();
        certificado.setCaminhoAssinaturaVisual("\0");
        assertNotFound();
    }

    @Test
    void assinaturaExistenteRetornaImagemComMimeCorretoNasDuasRotas() throws Exception {
        byte[] conteudo = {1, 2, 3};
        Certificado certificado = new Certificado();
        when(certificados.procurarPorId(7L)).thenReturn(Optional.of(certificado));

        for (String extensao : new String[] {"png", "jpg"}) {
            Path arquivo = diretorio.resolve("assinatura." + extensao);
            Files.write(arquivo, conteudo);
            certificado.setCaminhoAssinaturaVisual(arquivo.toString());
            MediaType esperado = extensao.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

            assertImagem(admin.exibirImagemDaPasta(7L), esperado, conteudo);
            assertImagem(publico.exibirImagemPublica(7L), esperado, conteudo);
        }
    }

    private void assertNotFound() {
        assertThat(admin.exibirImagemDaPasta(7L).getStatusCode().value()).isEqualTo(404);
        assertThat(publico.exibirImagemPublica(7L).getStatusCode().value()).isEqualTo(404);
    }

    private void assertImagem(ResponseEntity<Resource> resposta, MediaType mime, byte[] conteudo) throws Exception {
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(mime);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getInputStream().readAllBytes()).containsExactly(conteudo);
    }
}
