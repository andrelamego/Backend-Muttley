package com.fatec.muttley.evento;

import com.fatec.muttley.auth.CadastroConviteService;
import com.fatec.muttley.certificado.AssinaturaStorage;
import com.fatec.muttley.certificado.Certificado;
import com.fatec.muttley.certificado.CertificadoService;
import com.fatec.muttley.email.EmailProducer;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.medalha.MedalhaService;
import com.fatec.muttley.participacao.AtualizacaoParticipacao;
import com.fatec.muttley.participacao.AtualizacaoParticipacaoNovoEvento;
import com.fatec.muttley.participacao.InscricaoPublicaRequest;
import com.fatec.muttley.participacao.Participacao;
import com.fatec.muttley.participacao.ParticipacaoComEventoResponse;
import com.fatec.muttley.participacao.ParticipacaoService;
import com.fatec.muttley.qrcode.QrCodeClient;
import com.fatec.muttley.qrcode.QrCodeProducer;
import com.fatec.muttley.qrcode.dto.QrCodeRequest;
import com.fatec.muttley.qrcode.dto.TipoQrCode;
import io.github.andrelamego.brValidator.cpf.ValidCpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Eventos", description = "Endpoints públicos e administrativos de gerenciamento de eventos, inscrições e presenças")
@RestController
public class EventoController {

    private final EventoService eventoService;

    private final EventoMapper eventoMapper;

    private final ParticipacaoService participacaoService;

    private final QrCodeProducer qrCodeProducer;

    private final QrCodeClient qrCodeClient;

    private final CertificadoService certificadoService;

    private final MedalhaService medalhaService;

    private final EmailProducer emailProducer;

    private final CadastroConviteService cadastroConviteService;

    private final String frontendUrl;

    private final AssinaturaStorage assinaturaStorage;

    private final Clock clock;

    public EventoController(EventoService eventoService, EventoMapper eventoMapper,
            ParticipacaoService participacaoService, QrCodeProducer qrCodeProducer, QrCodeClient qrCodeClient,
            CertificadoService certificadoService, MedalhaService medalhaService, EmailProducer emailProducer,
            CadastroConviteService cadastroConviteService,
            @Value("${app.frontend.url}") String frontendUrl, AssinaturaStorage assinaturaStorage, Clock clock) {
        this.eventoService = eventoService;
        this.eventoMapper = eventoMapper;
        this.participacaoService = participacaoService;
        this.qrCodeProducer = qrCodeProducer;
        this.qrCodeClient = qrCodeClient;
        this.certificadoService = certificadoService;
        this.medalhaService = medalhaService;
        this.emailProducer = emailProducer;
        this.cadastroConviteService = cadastroConviteService;
        this.frontendUrl = frontendUrl;
        this.assinaturaStorage = assinaturaStorage;
        this.clock = clock;
    }

    @Operation(summary = "Listar eventos públicos disponíveis", description = "Retorna os eventos abertos ou visíveis para inscrição pública.")
    @ApiResponse(responseCode = "200", description = "Lista de eventos públicos retornada com sucesso")
    @GetMapping("/api/eventos")
    public ResponseEntity<List<EventoPublicoResponse>> listarEventosPublicos() {
        List<EventoPublicoResponse> eventos = eventoService.procurarDisponiveisParaInscricao().stream()
                .map(evento -> EventoPublicoResponse.from(evento, HorariosEvento.jaIniciou(evento, clock)))
                .toList();
        return ResponseEntity.ok(eventos);
    }

    @Operation(summary = "Buscar evento público por ID", description = "Retorna detalhes públicos de um evento específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/api/eventos/{id}")
    public ResponseEntity<EventoPublicoResponse> buscarEventoPublico(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        return ResponseEntity.ok(EventoPublicoResponse.from(evento, HorariosEvento.jaIniciou(evento, clock)));
    }

    @Operation(summary = "Realizar inscrição pública em evento", description = "Inscreve um participante com nome, email e CPF no evento especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscrição realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de inscrição inválidos"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PostMapping("/api/eventos/{id}/inscricoes")
    @Transactional
    public ResponseEntity<Map<String, Object>> registrarInscricaoPublica(
            @Parameter(description = "ID do evento") @PathVariable Long id,
            @RequestBody @Valid InscricaoPublicaRequest dados) {
        Participacao participacao = participacaoService.registrarInscricaoPublica(id, dados);

        emailProducer.publicarConfirmacaoInscricao(participacao);
        if(participacao.getPessoa().getSenha() == null){
            cadastroConviteService.emitir(participacao.getPessoa())
                    .ifPresent(token -> emailProducer.publicarCompletarCadastro(participacao, frontendUrl, token));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Inscricao realizada com sucesso.",
                "participacaoId", participacao.getId(),
                "inscricao", participacao.getInscricao()
        ));
    }

    @Operation(summary = "Listar eventos (Administração)",
            description = "Retorna página filtrada e ordenada de eventos para a área administrativa.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de eventos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/api/admin/eventos")
    public ResponseEntity<Page<Evento>> listarEventos(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "data") String ordenar,
            @RequestParam(required = false) StatusEventoEnum status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {

        Sort sort = ordenar.equals("tema")
                ? Sort.by("tema").ascending()
                : Sort.by("data").ascending().and(Sort.by("horarioInicio").ascending());

        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        return ResponseEntity.ok(eventoService.procurarProximosFiltrados(busca, status, pageable));
    }

    @Operation(summary = "Buscar evento por ID (Administração)",
            description = "Retorna os detalhes completos do evento para edição ou visualização detalhada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/api/admin/eventos/{id}")
    public ResponseEntity<AtualizacaoEvento> buscarPorId(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        return ResponseEntity.ok(eventoMapper.toAtualizacaoDto(evento));
    }

    @Operation(summary = "Criar novo evento com participantes",
            description = "Cadastra um novo evento e suas participações, enfileirando a geração dos QR Codes.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do evento inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping("/api/admin/eventos")
    @Transactional
    public ResponseEntity<Map<String, Object>> criar(@RequestBody @Valid EventoComParticipacaoDTO dto) {
        AtualizacaoEvento dtoEvento = dto.evento().withId(null);
        List<AtualizacaoParticipacaoNovoEvento> dtoNovasParticipacoes = dto.participacoes();

        Evento eventoSalvo = eventoService.salvarOuAtualizar(dtoEvento);

        if(dtoNovasParticipacoes != null) {
            for (AtualizacaoParticipacaoNovoEvento participacao : dtoNovasParticipacoes) {
                AtualizacaoParticipacao dtoParticipacao = new AtualizacaoParticipacao(
                        participacao.id(),
                        participacao.inscricao(),
                        participacao.tipo(),
                        participacao.pessoaId(),
                        eventoSalvo.getId()
                );

                participacaoService.salvarOuAtualizar(dtoParticipacao);
            }
        }

        qrCodeProducer.publicarQrCodeInscricao(eventoSalvo, frontendUrl);
        qrCodeProducer.publicarQrCodeConfirmacao(eventoSalvo, frontendUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Evento '" + eventoSalvo.getTema() + "' criado com sucesso.",
                "id", eventoSalvo.getId()
        ));
    }

    @Operation(summary = "Atualizar evento existente",
            description = "Atualiza os dados de um evento e sua lista de participações.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PutMapping("/api/admin/eventos/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID do evento") @PathVariable Long id,
            @RequestBody @Valid EventoComParticipacaoDTO dto) {
        eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));

        AtualizacaoEvento dtoEvento = dto.evento().withId(id);
        List<AtualizacaoParticipacaoNovoEvento> dtoNovasParticipacoes = dto.participacoes();

        Evento eventoSalvo = eventoService.salvarOuAtualizar(dtoEvento);

        if(dtoNovasParticipacoes != null) {
            for (AtualizacaoParticipacaoNovoEvento participacao : dtoNovasParticipacoes) {
                AtualizacaoParticipacao dtoParticipacao = new AtualizacaoParticipacao(
                        participacao.id(),
                        participacao.inscricao(),
                        participacao.tipo(),
                        participacao.pessoaId(),
                        eventoSalvo.getId()
                );

                participacaoService.salvarOuAtualizar(dtoParticipacao);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Evento '" + eventoSalvo.getTema() + "' atualizado com sucesso."));
    }

    @Operation(summary = "Cancelar evento",
            description = "Cancela um evento e notifica todos os participantes inscritos por email.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento cancelado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @DeleteMapping("/api/admin/eventos/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> cancelar(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));

        List<Participacao> inscritos = participacaoService.procurarPorEvento(id);

        eventoService.cancelarEvento(id);

        emailProducer.publicarEventoCancelado(evento, inscritos);
        return ResponseEntity.ok(Map.of("message", "Evento cancelado com sucesso."));
    }

    @Operation(summary = "Baixar QR Code de Inscrição",
            description = "Gera e faz o download da imagem PNG do QR Code que direciona para a página de inscrição do evento.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem do QR Code retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado"),
            @ApiResponse(responseCode = "503", description = "Serviço de QR Code indisponível")
    })
    @GetMapping("/api/admin/eventos/{id}/qrcode-inscricao")
    public ResponseEntity<byte[]> baixarQrCodeInscricao(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        return gerarQrCode(evento, TipoQrCode.INSCRICAO);
    }

    @Operation(summary = "Baixar QR Code de Confirmação de Presença",
            description = "Gera e faz o download da imagem PNG do QR Code que direciona para a confirmação presencial de presença do evento.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem do QR Code retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado"),
            @ApiResponse(responseCode = "503", description = "Serviço de QR Code indisponível")
    })
    @GetMapping("/api/admin/eventos/{id}/qrcode-confirmacao")
    public ResponseEntity<byte[]> baixarQrCodeConfirmacao(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        return gerarQrCode(evento, TipoQrCode.CONFIRMACAO);
    }

    private ResponseEntity<byte[]> gerarQrCode(Evento evento, TipoQrCode tipo) {
        try {
            byte[] imagem = qrCodeClient.gerarQrCode(new QrCodeRequest(evento.getId(), frontendUrl, evento.getTema(), tipo));
            String finalidade = tipo == TipoQrCode.INSCRICAO ? "inscricao" : "confirmacao";
            String nomeArquivo = "qrcode-" + finalidade + "-" + evento.getTema()
                    .replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("(^-|-$)", "").toLowerCase() + ".png";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imagem);
        } catch (Exception erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de QR Code indisponível. Tente novamente em alguns instantes.", erro);
        }
    }

    @Operation(summary = "Consultar participações para fechamento do evento",
            description = "Retorna a lista de todas as participações registradas no evento para conferência e chamada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participações retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @GetMapping("/api/admin/eventos/{id}/participacoes")
    public ResponseEntity<Map<String, Object>> dadosConclusao(
            @Parameter(description = "ID do evento") @PathVariable Long id) {
        Evento evento = eventoService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));

        return ResponseEntity.ok(Map.of(
                "participacoes", participacaoService.procurarPorEvento(id).stream()
                        .map(ParticipacaoComEventoResponse::from)
                        .toList()
        ));
    }

    @Operation(summary = "Concluir evento e emitir certificados",
            description = "Finaliza o evento que esteja EM_ANDAMENTO, marca presenças, gera medalhas de presença e emite certificados com a assinatura digital.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento concluído e certificados gerados"),
            @ApiResponse(responseCode = "400", description = "Evento não está em andamento"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    @PostMapping(value = "/api/admin/eventos/{id}/concluir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> concluirEvento(
            @Parameter(description = "ID do evento") @PathVariable Long id,
            @Parameter(description = "Lista opcional de IDs de participações presentes") @RequestParam(value = "presentes", required = false) List<Long> presentes,
            @Parameter(description = "Arquivo de imagem da assinatura do certificado") @RequestParam(value = "file") MultipartFile file) {

        Evento evento = eventoService.procurarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));

        if (evento.getStatus() != StatusEventoEnum.EM_ANDAMENTO) {
            throw new IllegalStateException("O evento só pode ser concluído quando estiver EM_ANDAMENTO.");
        }

        String caminhoAssinatura = assinaturaStorage.salvar(file);

        List<Participacao> participacoesDoEvento = participacaoService.procurarPorEvento(id);
        Set<Long> participacoesValidas = participacoesDoEvento.stream()
                .map(Participacao::getId)
                .collect(Collectors.toSet());

        if (presentes != null) {
            presentes.stream()
                    .filter(participacoesValidas::contains)
                    .forEach(participacaoService::marcarPresente);
        }

        Set<Long> todosPresentes = participacaoService.procurarPorEvento(id).stream()
                .filter(Participacao::isPresente)
                .map(Participacao::getId)
                .collect(Collectors.toSet());

        medalhaService.gerarMedalhasBronzePorPresenca(
                participacaoService.procurarPorEvento(id)
        );

        List<Certificado> certificadosEmail = certificadoService
                .gerarCertificadosParaParticipacoes(todosPresentes.stream().sorted().toList(), caminhoAssinatura);

        eventoService.concluirEvento(id);

        List<Participacao> inscritos = participacaoService.procurarPorEvento(id);
        emailProducer.publicarEventoConcluido(evento, inscritos);
        emailProducer.publicarCertificados(certificadosEmail, frontendUrl);

        return ResponseEntity.ok(Map.of("message", "Evento concluído e certificados gerados com sucesso."));
    }

    @Operation(summary = "Confirmar presença pública por CPF",
            description = "Confirma a presença presencial do participante através do CPF informado e gera medalha de presença.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presença confirmada com sucesso"),
            @ApiResponse(responseCode = "400", description = "CPF inválido ou regra violada"),
            @ApiResponse(responseCode = "404", description = "Participação ou evento não encontrado")
    })
    @PostMapping("/api/eventos/{eventoId}/confirmar-presenca/{cpf}")
    @Transactional
    public ResponseEntity<String> confirmarPresenca(
            @Parameter(description = "ID do evento") @PathVariable Long eventoId,
            @Parameter(description = "CPF válido do participante") @PathVariable @ValidCpf String cpf) {

        Participacao participacao = participacaoService.confirmarPresenca(eventoId, cpf);
        medalhaService.gerarMedalhaBronzePorPresenca(participacao);
        return ResponseEntity.ok("Presença confirmada com sucesso!");
    }
}
