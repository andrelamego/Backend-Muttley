package com.fatec.muttley.massa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class MassaDadosService {
    private static final Logger LOG = LoggerFactory.getLogger(MassaDadosService.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String diretorioAssinaturas;

    public MassaDadosService(JdbcTemplate jdbc, PasswordEncoder encoder, Clock clock, String diretorioAssinaturas) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.clock = clock;
        this.diretorioAssinaturas = diretorioAssinaturas;
    }

    @Transactional(rollbackFor = IOException.class)
    public boolean carregar() throws IOException {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM pessoa WHERE email='admin.massa@example.test'", Long.class) > 0) {
            LOG.info("Massa já carregada; registros e agendas mantidos.");
            return false;
        }
        verificarBaseVazia();
        String assinatura = AssinaturaMassa.criar(diretorioAssinaturas).toString();
        LocalDateTime agora = LocalDateTime.now(clock.withZone(FUSO)).truncatedTo(ChronoUnit.MINUTES);
        criarPessoas();
        criarCadastros();
        List<EventoMassa> eventos = criarEventos(agora);
        int numeroInscricao = 0;
        int numeroMedalha = 0;
        int numeroCertificado = 0;
        for (EventoMassa evento : eventos) {
            for (int indice = 0; indice < evento.inscritos(); indice++) {
                long pessoa = 10003 + (indice + evento.indice() * 7) % 64;
                // Ana tem histórico e participa dos cenários principais; não repete pessoa/evento.
                if (indice == 0) pessoa = 10003;
                else if (pessoa == 10003) pessoa = 10003 + (evento.indice() * 7 + evento.inscritos()) % 64;
                boolean presente = indice < evento.presentes();
                long participacao = 50000 + ++numeroInscricao;
                String tipo = indice == 1 ? "Palestrante" : indice == 2 ? "Organizador" : "Participante";
                inserir("participacao", "id_participacao,inscricao,tipo,presente,id_evento,id_pessoa",
                        participacao, numeroInscricao, tipo, presente, evento.id(), pessoa);
                if (presente) {
                    inserir("medalha", "id_medalha,nome,descricao,tipo,id_participacao,participacao_presenca_id",
                            70000 + ++numeroMedalha, "Participação confirmada", "Bronze por presença no evento — massa de testes.",
                            "BRONZE", participacao, participacao);
                    if (evento.status().equals("FINALIZADO")) {
                        String codigo = UUID.nameUUIDFromBytes(("massa-muttley:" + participacao)
                                .getBytes(StandardCharsets.UTF_8)).toString();
                        inserir("certificado", "id_certificado,data_emissao,assinatura,codigo_validacao,url_publica,"
                                        + "caminho_pdf,caminho_assinatura_visual,id_participacao",
                                60000 + ++numeroCertificado, evento.data(), "Coordenação de testes — Muttley",
                                codigo, "/certificados/" + codigo, "/certificados/" + codigo + ".pdf", assinatura, participacao);
                    }
                    if (indice == 0 && evento.status().equals("FINALIZADO")) {
                        inserir("medalha", "id_medalha,nome,descricao,tipo,id_participacao",
                                70000 + ++numeroMedalha, "Destaque acadêmico", "Reconhecimento manual fictício.",
                                evento.indice() % 2 == 0 ? "PRATA" : "OURO", participacao);
                    }
                }
            }
        }
        // Cadastro parcial também participa dos cenários de presença e ativação.
        inserir("participacao", "id_participacao,inscricao,tipo,presente,id_evento,id_pessoa",
                50000 + ++numeroInscricao, numeroInscricao, "Participante", false, 40015, 10067);
        inserir("participacao", "id_participacao,inscricao,tipo,presente,id_evento,id_pessoa",
                50000 + ++numeroInscricao, numeroInscricao, "Participante", false, 40005, 10068);
        jdbc.update("UPDATE sequencia_inscricao SET valor=? WHERE id=1", numeroInscricao);
        LOG.info("Massa criada em {}: 72 pessoas, 36 eventos, {} participações, {} certificados e {} medalhas. "
                + "Credenciais e cenários: docs/operacao/massa-de-testes.md", agora, numeroInscricao,
                numeroCertificado, numeroMedalha);
        return true;
    }

    private void verificarBaseVazia() {
        for (String tabela : List.of("aluno", "professor", "palestrante", "organizador", "colaborador", "endereco",
                "local", "disciplina", "patrocinador", "evento", "participacao", "certificado", "medalha")) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class) > 0) {
                throw new IllegalStateException("A massa exige uma base vazia; já existem registros em " + tabela
                        + ". Recrie o banco local conforme docs/operacao/massa-de-testes.md.");
            }
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM pessoa WHERE role IS NULL OR role<>'ADMIN' OR id_pessoa>=10000",
                Long.class) > 0) {
            throw new IllegalStateException("A massa aceita somente administradores do bootstrap na base inicial.");
        }
    }

    private void criarPessoas() {
        String senha = encoder.encode(CatalogoMassa.SENHA);
        pessoa(10001, "Administrador da massa", "admin.massa@example.test", 1, senha, "ADMIN");
        pessoa(10002, "Administradora de eventos", "gestao.massa@example.test", 2, senha, "ADMIN");
        for (int indice = 0; indice < 64; indice++) {
            String nome = CatalogoMassa.NOMES[indice % 8] + " " + CatalogoMassa.SOBRENOMES[indice / 8];
            String email = indice == 0 ? "ana.massa@example.test" : indice == 1 ? "bruno.massa@example.test"
                    : String.format(Locale.ROOT, "usuario%02d.massa@example.test", indice + 1);
            pessoa(10003 + indice, nome, email, indice + 3, senha, "USER");
        }
        pessoa(10067, "Cadastro com convite válido", "convite.valido@example.test", 67, null, "USER");
        pessoa(10068, "Cadastro com convite expirado", "convite.expirado@example.test", 68, null, "USER");
        pessoa(10069, "Cadastro ainda não ativado", "sem.convite@example.test", 69, null, "USER");
        pessoa(10070, "Cadastro já ativado", "convite.consumido@example.test", 70, senha, "USER");
        pessoa(10071, "Participante sem histórico", "vazio.massa@example.test", 71, senha, "USER");
        pessoa(10072, "Participante para nova inscrição", "novo.massa@example.test", 72, senha, "USER");
        jdbc.update("UPDATE pessoa SET telefone=NULL WHERE id_pessoa IN (10067,10068,10069)");
        jdbc.update("UPDATE pessoa SET cpf=NULL WHERE id_pessoa=10069");
        convite(10067, CatalogoMassa.CONVITE_VALIDO, clock.instant().plus(24, ChronoUnit.HOURS));
        convite(10068, CatalogoMassa.CONVITE_EXPIRADO, clock.instant().minus(26, ChronoUnit.HOURS));
    }

    private void pessoa(long id, String nome, String email, int indice, String senha, String role) {
        inserir("pessoa", "id_pessoa,nome,email,telefone,cpf,senha,role", id, nome, email,
                String.format(Locale.ROOT, "119%08d", 10000000 + indice), CatalogoMassa.cpf(indice), senha, role);
    }

    private void convite(long pessoa, String token, Instant expira) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
            jdbc.update("UPDATE pessoa SET cadastro_token_hash=?,cadastro_token_expira_em=? WHERE id_pessoa=?",
                    hash, LocalDateTime.ofInstant(expira, ZoneOffset.UTC), pessoa);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private void criarCadastros() {
        for (int indice = 0; indice < 48; indice++) {
            inserir("aluno", "id_pessoa,instituicao,matricula", 10003 + indice,
                    indice % 2 == 0 ? "FATEC — instituição fictícia de testes" : "Instituto de Tecnologia — testes",
                    "TESTE-" + (20260001 + indice));
        }
        for (int indice = 0; indice < 8; indice++) {
            inserir("professor", "id_pessoa,area_formacao,titulacao", 10051 + indice,
                    CatalogoMassa.DISCIPLINAS.get(indice), indice % 2 == 0 ? "Mestrado" : "Doutorado");
        }
        for (int indice = 0; indice < 6; indice++) {
            inserir("palestrante", "id_pessoa,resumo_profissional,empresa_atual,cargo", 10053 + indice,
                    "Perfil fictício com experiência em projetos acadêmicos e oficinas técnicas.",
                    "Empresa de testes " + (indice + 1), "Especialista em tecnologia");
        }
        for (long pessoa : List.of(10001L, 10059L, 10060L, 10061L, 10062L)) {
            inserir("organizador", "id_pessoa,instituicao,cargo", pessoa, "Coordenação de eventos — testes", "Organizador");
        }
        for (long pessoa = 10063; pessoa <= 10066; pessoa++) {
            inserir("colaborador", "id_pessoa,funcao,disponibilidade,tipo", pessoa,
                    "Apoio e recepção", "Manhã e noite", "Voluntário");
        }
        for (int indice = 0; indice < 6; indice++) {
            inserir("endereco", "id_endereco,estado,cidade,bairro,logradouro,numero,complemento",
                    20001 + indice, "SP", indice < 4 ? "São Paulo" : "Guarulhos", "Bairro de testes " + (indice + 1),
                    "Rua fictícia " + (indice + 1), 100 + indice * 20, "Unidade de demonstração " + (indice + 1));
        }
        String[] locais = {"Auditório central", "Laboratório de programação", "Sala de oficina", "Anfiteatro",
                "Sala virtual", "Sala acessível", "Laboratório de integração"};
        int[] capacidades = {120, 24, 6, 80, 200, 16, 12};
        for (int indice = 0; indice < locais.length; indice++) {
            inserir("local", "id_local,nome,descricao,capacidade,id_endereco", 21001 + indice, locais[indice],
                    "Local fictício para testar capacidade, seleção e apresentação de eventos.", capacidades[indice], 20001 + indice % 6);
        }
        String[] turnos = {"MATUTINO", "VESPERTINO", "NORTUNO"};
        for (int indice = 0; indice < CatalogoMassa.DISCIPLINAS.size(); indice++) {
            inserir("disciplina", "id_disciplina,nome,descricao,turno,id_professor", 22001 + indice,
                    CatalogoMassa.DISCIPLINAS.get(indice), "Disciplina de demonstração com atividades práticas e palestras.",
                    turnos[indice % 3], 10051 + indice % 8);
        }
        for (int indice = 0; indice < 8; indice++) {
            inserir("patrocinador", "id_patrocinador,nome,cnpj,valor_patrocinio,email,telefone,site", 23001 + indice,
                    "Parceiro de testes " + (indice + 1), CatalogoMassa.cnpj(indice + 1), 500.0 + indice * 750,
                    "parceiro" + (indice + 1) + "@example.test", "113000" + (1000 + indice),
                    "https://parceiro" + (indice + 1) + ".example.test");
        }
    }

    private record EventoMassa(int indice, long id, LocalDate data, String status, int inscritos, int presentes) {}

    private List<EventoMassa> criarEventos(LocalDateTime agora) {
        List<EventoMassa> eventos = new ArrayList<>();
        int[] dias = {1, 2, 3, 4, 5, 7, 9, 11, 14, 21, 30, 45};
        int[] locais = {0, 2, 2, 1, 3, 4, 5, 6, 0, 1, 3, 4};
        int[] inscritos = {0, 6, 5, 12, 28, 32, 10, 8, 30, 16, 24, 36};
        for (int indice = 0; indice < dias.length; indice++) {
            String tema = indice == 0 ? "Evento sem inscrições — " : indice == 1 ? "Evento lotado — "
                    : indice == 2 ? "Última vaga — " : "";
            adicionarEvento(eventos, tema + CatalogoMassa.TEMAS.get(indice), agora.toLocalDate().plusDays(dias[indice]),
                    LocalTime.of(9 + indice % 3 * 3, 0), LocalTime.of(11 + indice % 3 * 3, 0),
                    "CRIADO", locais[indice], inscritos[indice], 0);
        }
        LocalDateTime proximo = futuroMesmoDiaOuAmanha(agora, 30);
        adicionarEvento(eventos, "Inscrições abertas para o próximo horário", proximo.toLocalDate(), proximo.toLocalTime(),
                fim(proximo.toLocalTime()), "CRIADO", 0, 8, 0);
        LocalDateTime tolerancia = futuroMesmoDiaOuAmanha(agora, 5);
        adicionarEvento(eventos, "Presença liberada antes do início — tolerância", tolerancia.toLocalDate(), tolerancia.toLocalTime(),
                fim(tolerancia.toLocalTime()), "CRIADO", 1, 8, 0);
        LocalTime inicio = agora.toLocalTime().isBefore(LocalTime.of(0, 30)) ? LocalTime.MIDNIGHT : agora.toLocalTime().minusMinutes(30);
        LocalTime termino = fim(agora.toLocalTime());
        adicionarEvento(eventos, "Em andamento — confirmar presença e concluir", agora.toLocalDate(), inicio, termino,
                "EM_ANDAMENTO", 1, 20, 12);
        adicionarEvento(eventos, "Em andamento — oficina prática", agora.toLocalDate(), inicio, termino,
                "EM_ANDAMENTO", 5, 12, 6);
        adicionarEvento(eventos, "Aguardando conclusão — janela de presença encerrada", agora.toLocalDate().minusDays(1),
                LocalTime.of(14, 0), LocalTime.of(16, 0), "EM_ANDAMENTO", 3, 28, 18);
        adicionarEvento(eventos, "Aguardando conclusão — participantes ausentes", agora.toLocalDate().minusDays(3),
                LocalTime.of(9, 0), LocalTime.of(12, 0), "EM_ANDAMENTO", 0, 20, 14);
        int[] historico = {1, 3, 5, 9, 15, 21, 32, 40, 65, 90, 120, 200};
        for (int indice = 0; indice < historico.length; indice++) {
            int total = 12 + indice % 4 * 4;
            adicionarEvento(eventos, "Edição concluída — " + CatalogoMassa.TEMAS.get(indice),
                    agora.toLocalDate().minusDays(historico[indice]), LocalTime.of(14, 0), LocalTime.of(17, 0),
                    "FINALIZADO", indice % 2 == 0 ? 0 : 4, total, total - total / 3);
        }
        for (int indice = 0; indice < 6; indice++) {
            adicionarEvento(eventos, "Edição cancelada — " + CatalogoMassa.TEMAS.get(indice),
                    agora.toLocalDate().plusDays(indice < 4 ? indice + 6 : -indice - 4), LocalTime.of(10, 0), LocalTime.of(12, 0),
                    "CANCELADO", 0, indice == 0 ? 0 : 6 + indice * 2, 0);
        }
        return eventos;
    }

    private LocalDateTime futuroMesmoDiaOuAmanha(LocalDateTime agora, int minutos) {
        LocalDateTime horario = agora.plusMinutes(minutos);
        return horario.toLocalTime().isAfter(LocalTime.of(23, 57))
                ? horario.toLocalDate().plusDays(1).atTime(0, 2) : horario;
    }

    private LocalTime fim(LocalTime inicio) {
        return inicio.isAfter(LocalTime.of(22, 59)) ? LocalTime.of(23, 59) : inicio.plusHours(1);
    }

    private void adicionarEvento(List<EventoMassa> eventos, String tema, LocalDate data, LocalTime inicio, LocalTime fim,
            String status, int local, int inscritos, int presentes) {
        int indice = eventos.size();
        long id = 40001 + indice;
        inserir("evento", "id_evento,tema,data,horario_inicio,horario_fim,descricao,modalidade,status,"
                        + "id_disciplina,id_patrocinador,id_local",
                id, tema, data, inicio.toString(), fim.toString(),
                "Evento fictício da massa Muttley. Atividade com conteúdo introdutório, discussão de casos, "
                        + "exercícios práticos e espaço para dúvidas. Permite testar inscrições, presença e certificados.",
                local == 4 ? "ONLINE" : "PRESENCIAL", status, 22001 + indice % 12, 23001 + indice % 8, 21001 + local);
        eventos.add(new EventoMassa(indice, id, data, status, inscritos, presentes));
    }

    private void inserir(String tabela, String colunas, Object... valores) {
        String parametros = String.join(",", Collections.nCopies(valores.length, "?"));
        jdbc.update("INSERT INTO " + tabela + " (" + colunas + ") VALUES (" + parametros + ")", valores);
    }
}
