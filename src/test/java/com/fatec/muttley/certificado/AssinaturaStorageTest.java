package com.fatec.muttley.certificado;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static com.fatec.muttley.support.ImagensTeste.criar;
import static org.assertj.core.api.Assertions.*;

class AssinaturaStorageTest {
    @TempDir Path diretorio;
    @ParameterizedTest @CsvSource({"jpg,jpg,image/jpeg","jpeg,jpg,image/jpeg","PNG,png,image/png"})
    void aceitaImagemRealEIgnoraCaminhoDoNomeOriginal(String extensao,String formato,String mime) throws Exception {
        byte[] bytes=criar(formato);
        var file=new MockMultipartFile("file","../../assinatura."+extensao,mime,bytes);
        Path salvo=Path.of(new AssinaturaStorage(diretorio.toString()).salvar(file));
        assertThat(salvo.getParent()).isEqualTo(diretorio);
        assertThat(salvo.getFileName().toString()).doesNotContain("assinatura","..");
        assertThat(Files.readAllBytes(salvo)).containsExactly(bytes);
    }
    @ParameterizedTest @ValueSource(strings={"text/plain","image/gif","application/pdf","application/octet-stream"})
    void rejeitaMimeIncompativelMesmoComPngReal(String mime) {
        var file=new MockMultipartFile("file","assinatura.png",mime,criar("png"));
        assertThatThrownBy(() -> new AssinaturaStorage(diretorio.toString()).salvar(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
    @Test void rejeitaArquivoNulo() {
        assertThatThrownBy(() -> new AssinaturaStorage(diretorio.toString()).salvar(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
    @Test void falhaDeEscritaRetorna500SemDeixarArquivoParcial() throws Exception {
        Path arquivo=Files.writeString(diretorio.resolve("nao-e-diretorio"),"teste");
        var file=new MockMultipartFile("file","assinatura.png","image/png",criar("png"));
        assertThatThrownBy(() -> new AssinaturaStorage(arquivo.toString()).salvar(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(500));
        assertThat(Files.readString(arquivo)).isEqualTo("teste");
    }
}
