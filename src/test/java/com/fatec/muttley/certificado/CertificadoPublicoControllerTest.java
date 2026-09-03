package com.fatec.muttley.certificado;

import com.fatec.muttley.pdf.PdfClient;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificadoPublicoControllerTest {
    @Mock CertificadoService certificados; @Mock PdfClient pdf;
    @InjectMocks CertificadoPublicoController controller;
    @TempDir Path diretorio;
    Certificado certificado;
    @BeforeEach void setup() {
        var resolver=new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine=new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        ReflectionTestUtils.setField(controller,"templateEngine",engine);
        certificado=new Certificado(); certificado.setCodigoValidacao("codigo"); certificado.setParticipacao(participacao(1,true));
        certificado.setDataEmissao(LocalDate.of(2026,9,3));
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void RF_CER_04_05_RN_CER_08_09_10_pdfUsaTemplateRealEAssinaturaEmbutida(boolean preview) throws Exception {
        Path assinatura=diretorio.resolve("assinatura.png");Files.write(assinatura,new byte[]{1,2,3});
        certificado.setCaminhoAssinaturaVisual(assinatura.toString());
        certificado.getParticipacao().getPessoa().setNome("Nome <script>alert(1)</script>");
        when(certificados.procurarPorCodigoValidacao("codigo")).thenReturn(Optional.of(certificado));
        when(pdf.gerarPdf(anyString())).thenReturn(new byte[]{37,80,68,70});
        var response=preview?controller.preview("codigo",new ExtendedModelMap()):controller.download("codigo",new ExtendedModelMap());
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo(preview?"inline":"attachment");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4);
        assertThat(response.getBody()).containsExactly((byte)37,(byte)80,(byte)68,(byte)70);
        ArgumentCaptor<String> html=ArgumentCaptor.forClass(String.class);verify(pdf).gerarPdf(html.capture());
        assertThat(html.getValue()).contains("data:image/png;base64,AQID","Semana de tecnologia","2h30.","03/09/2026")
                .contains("&lt;script&gt;").doesNotContain("<script>alert(1)</script>");
    }
    @ParameterizedTest @CsvSource({"09:00,10:00,1 hora.","09:00,11:00,2 horas.","09:00,09:45,0h45.","09:00,11:30,2h30.","10:00,09:00,carga horária não informada.","errado,09:00,carga horária não informada."})
    void RN_CER_12_calculaCargaHoraria(String inicio,String fim,String esperado) throws Exception {
        certificado.getParticipacao().getEvento().setHorarioInicio(inicio); certificado.getParticipacao().getEvento().setHorarioFim(fim);
        when(certificados.procurarPorCodigoValidacao("codigo")).thenReturn(Optional.of(certificado));
        when(pdf.gerarPdf(anyString())).thenReturn(new byte[]{1});
        var model=new ExtendedModelMap();controller.preview("codigo",model);
        assertThat(model.get("duracao")).isEqualTo(esperado);
    }
    @Test void RF_CER_09_linkedInIncluiCodigoDataEUrlPublica() {
        when(certificados.procurarPorCodigoValidacao("codigo")).thenReturn(Optional.of(certificado));
        var request=new MockHttpServletRequest();request.setScheme("https");request.setServerName("muttley.example.invalid");request.setServerPort(443);request.setRequestURI("/api/certificados/codigo");
        String link=(String)controller.dadosCertificadoPublico("codigo",request).getBody().get("linkedinUrl");
        assertThat(link).startsWith("https://www.linkedin.com/profile/add?").contains("certId=codigo","issueYear=2026","issueMonth=9","/certificados/codigo");
    }
    @Test void codigoDesconhecidoRetorna404SemChamarPdf() {
        assertThatThrownBy(() -> controller.preview("desconhecido",new ExtendedModelMap()))
                .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(pdf);
    }
}
