package com.fatec.muttley;

import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.certificado.*;
import com.fatec.muttley.medalha.MedalhaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock EventoService eventos; @Mock CertificadoService certificados; @Mock MedalhaService medalhas;
    @InjectMocks AdminController controller;
    @ParameterizedTest @CsvSource({"0,0,0","5,0,100","15,10,50","5,10,-50","0,10,-100","10,10,0","4,3,33"})
    void dashboardCalculaVariacaoEPeriodos(long atual,long anterior,long esperado) {
        when(certificados.contarEmitidosDesde(any())).thenReturn(atual);
        when(certificados.contarEmitidosEntre(any(),any())).thenReturn(anterior);
        LocalDate antes=LocalDate.now(); var body=controller.carregarEstatisticasAdmin().getBody();
        assertThat(body.get("variacaoCertificadosUltimos30Dias")).isEqualTo(esperado);
        assertThat(body.get("certificadosUltimos30Dias")).isEqualTo(atual);
        ArgumentCaptor<LocalDate> inicio=ArgumentCaptor.forClass(LocalDate.class);
        verify(certificados).contarEmitidosDesde(inicio.capture());
        assertThat(inicio.getValue()).isBetween(antes.minusDays(30),LocalDate.now().minusDays(30));
        verify(certificados).contarEmitidosEntre(inicio.getValue().minusDays(30),inicio.getValue());
        verify(eventos).contarEventosAtivosNoPeriodo(inicio.getValue().plusDays(30),inicio.getValue().plusDays(37));
        verify(eventos).contarEventosAtivos();
    }
    @Test void dashboardNormalizaBarrasEAplicaLimitesDoRanking() {
        var maior=mock(CertificadoRepository.CertificadosPorEvento.class);
        var menor=mock(CertificadoRepository.CertificadosPorEvento.class);
        when(maior.getTotal()).thenReturn(100L);when(maior.getEventoTema()).thenReturn("Maior");
        when(menor.getTotal()).thenReturn(1L);when(menor.getEventoTema()).thenReturn("Menor");
        when(certificados.procurarTotaisPorEvento(8)).thenReturn(List.of(maior,menor));
        var body=controller.carregarEstatisticasAdmin().getBody();
        assertThat(body.get("certificadosPorEvento")).isEqualTo(List.of(
                new AdminController.BarraEstatistica("Maior",100L,100),new AdminController.BarraEstatistica("Menor",1L,6)));
        verify(medalhas).procurarTotaisPorParticipante(7);
    }
}
