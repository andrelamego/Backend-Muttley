package com.fatec.muttley;

import com.fatec.muttley.certificado.CertificadoRepository;
import com.fatec.muttley.certificado.CertificadoService;
import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.conquista.ConquistaService;
import com.fatec.muttley.painel.PainelAdmin;
import com.fatec.muttley.painel.PainelAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Administração - Dashboard", description = "Painel administrativo com indicadores, gráficos e métricas do sistema")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EventoService eventoService;

    private final CertificadoService certificadoService;

    private final ConquistaService conquistaService;

    private final PainelAdminService painelAdminService;

    @Operation(summary = "Carregar dados e estatísticas do painel administrativo",
            description = "Retorna os próximos eventos, certificados emitidos, destaques do semestre, métricas dos últimos 30 dias, "
                    + "eventos de hoje, indicadores do semestre e do anterior, ocupação, presença recente, série mensal, "
                    + "presenças por disciplina e inscritos sem conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas recuperadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer papel ADMIN)")
    })
    @GetMapping("/inicio")
    public ResponseEntity<Map<String, Object>> carregarEstatisticasAdmin() {
        Map<String, Object> response = new HashMap<>();

        response.put("proximosEventos", eventoService.procurarTodosOrdenarPorInicio());

        List<CertificadoRepository.CertificadosPorEvento> certificadosPorEvento =
                certificadoService.procurarTotaisPorEvento(8);
        long maiorTotalCertificadosPorEvento = certificadosPorEvento.stream()
                .mapToLong(CertificadoRepository.CertificadosPorEvento::getTotal)
                .max()
                .orElse(0);

        LocalDate hoje = LocalDate.now();
        LocalDate inicioUltimos30Dias = hoje.minusDays(30);
        LocalDate inicioPeriodoAnterior = inicioUltimos30Dias.minusDays(30);
        long certificadosUltimos30Dias = certificadoService.contarEmitidosDesde(inicioUltimos30Dias);
        long certificadosPeriodoAnterior = certificadoService.contarEmitidosEntre(inicioPeriodoAnterior, inicioUltimos30Dias);

        response.put("certificadosPorEvento", certificadosPorEvento.stream()
                .map(item -> new BarraEstatistica(item.getEventoTema(), item.getTotal(), percentual(item.getTotal(), maiorTotalCertificadosPorEvento)))
                .toList());

        response.put("destaquesSemestre", conquistaService.destaquesDoSemestre(5));

        response.put("certificadosUltimos30Dias", certificadosUltimos30Dias);
        response.put("variacaoCertificadosUltimos30Dias", calcularVariacaoPercentual(certificadosUltimos30Dias, certificadosPeriodoAnterior));
        response.put("eventosAtivos", eventoService.contarEventosAtivos());
        response.put("eventosAtivosNaSemana", eventoService.contarEventosAtivosNoPeriodo(hoje, hoje.plusDays(7)));

        PainelAdmin.Resultado painel = painelAdminService.montar();
        response.put("hoje", painel.hoje());
        response.put("semestre", painel.semestre());
        response.put("semestreAnterior", painel.semestreAnterior());
        response.put("ocupacao", painel.ocupacao());
        response.put("presencaRecentes", painel.presencaRecentes());
        response.put("serieMensal", painel.serieMensal());
        response.put("porDisciplina", painel.porDisciplina());
        response.put("pessoasSemConta", painel.pessoasSemConta());

        return ResponseEntity.ok(response);
    }

    private int percentual(long valor, long maiorValor) {
        if (maiorValor <= 0) return 0;
        return Math.max(6, Math.round((valor * 100f) / maiorValor));
    }

    private long calcularVariacaoPercentual(long valorAtual, long valorAnterior) {
        if (valorAnterior == 0) return valorAtual > 0 ? 100 : 0;
        return Math.round(((valorAtual - valorAnterior) * 100f) / valorAnterior);
    }

    public record BarraEstatistica(String rotulo, long total, int percentual) {
    }
}