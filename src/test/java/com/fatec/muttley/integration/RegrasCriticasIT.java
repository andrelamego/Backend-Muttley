package com.fatec.muttley.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatec.muttley.certificado.*;
import com.fatec.muttley.email.dto.CadastroEmail;
import com.fatec.muttley.disciplina.*;
import com.fatec.muttley.evento.*;
import com.fatec.muttley.evento.enums.*;
import com.fatec.muttley.local.*;
import com.fatec.muttley.medalha.*;
import com.fatec.muttley.participacao.*;
import com.fatec.muttley.patrocinador.*;
import com.fatec.muttley.pdf.PdfClient;
import com.fatec.muttley.pessoa.*;
import com.fatec.muttley.pessoa.Role;
import com.fatec.muttley.qrcode.QrCodeClient;
import com.fatec.muttley.security.JwtService;
import com.fatec.muttley.support.ImagensTeste;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.community.dialect.MariaDBLegacyDialect;
import org.hibernate.dialect.MySQLDialect;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controllers, segurança, serviços, transações e MySQL reais. Só fronteiras externas são simuladas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RegrasCriticasIT.TempoConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegrasCriticasIT {
    @TempDir static Path arquivos;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("app.upload.assinaturas", () -> arquivos.toString());
    }
    static class Relogio extends Clock {
        private volatile Instant instante = Instant.parse("2026-09-03T13:00:00Z");
        void horario(String horario) { instante=LocalDateTime.parse("2026-09-03T"+horario).atZone(getZone()).toInstant(); }
        @Override public ZoneId getZone() { return ZoneId.of("America/Sao_Paulo"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instante,zone); }
        @Override public Instant instant() { return instante; }
    }
    @TestConfiguration static class TempoConfig {
        @Bean @Primary Relogio relogioDeTeste() { return new Relogio(); }
        @Bean @ServiceConnection JdbcDatabaseContainer<?> mysqlDeTeste() {
            if ("mariadb".equals(System.getProperty("muttley.test.database"))) {
                return new MariaDBContainer("mariadb:10.4.32").withDatabaseName("muttley_test").withUsername("test").withPassword("test");
            }
            return new MySQLContainer("mysql:8.4").withDatabaseName("muttley_test").withUsername("test").withPassword("test");
        }
    }
    @Autowired MockMvc mvc;
    @Autowired Relogio clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired EventoService eventoService;
    @Autowired TransactionTemplate tx;
    @Autowired PessoaRepository pessoas;
    @Autowired EventoRepository eventos;
    @Autowired LocalRepository locais;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired PatrocinadorRepository patrocinadores;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired CertificadoRepository certificados;
    @Autowired MedalhaRepository medalhas;
    @Autowired ParticipacaoService participacaoService;
    @Autowired MedalhaService medalhaService;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @MockitoBean(name="kafkaTemplate") KafkaTemplate kafka;
    @MockitoBean PdfClient pdf;
    @MockitoBean QrCodeClient qr;
    @MockitoSpyBean CertificadoService certificadoService;
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    Pessoa admin, ana, bia;


    @BeforeEach void preparar() {
        clock.horario("10:00:00");
        tx.executeWithoutResult(s -> {
            for(String tabela:List.of("certificado","medalha","participacao","evento","disciplina","patrocinador","local","pessoa","sequencia_inscricao"))
                jdbc.update("delete from "+tabela);
        });
        admin=pessoa("admin",Role.ADMIN,"52998224725");
        ana=pessoa("ana",Role.USER,"11144477735");
        bia=pessoa("bia",Role.USER,"12345678909");
        reset(kafka);
    }
    Pessoa pessoa(String nome,Role role,String cpf) {
        Pessoa p=new Pessoa();p.setNome(nome);p.setEmail(nome+"@example.invalid");p.setCpf(cpf);p.setTelefone("11999999999");p.setRole(role);p.setSenha(encoder.encode("Senha-forte-123"));return pessoas.saveAndFlush(p);
    }
    Evento evento(int capacidade,StatusEventoEnum status,String inicio,String fim) {
        return tx.execute(s -> {
            Local l=new Local();l.setNome("Auditório");l.setCapacidade(capacidade);locais.save(l);
            Disciplina d=new Disciplina();d.setNome("Programação");disciplinas.save(d);
            Patrocinador p=new Patrocinador();p.setNome("Patrocinador");patrocinadores.save(p);
            Evento e=new Evento();e.setTema("Semana acadêmica");e.setDescricao("Teste integrado");e.setData(LocalDate.of(2026,9,3));e.setHorarioInicio(inicio);e.setHorarioFim(fim);e.setStatus(status);e.setModalidade(ModalidadeEventoEnum.values()[0]);e.setLocal(l);e.setDisciplina(d);e.setPatrocinador(p);return eventos.saveAndFlush(e);
        });
    }
    Participacao inscrito(Evento e,Pessoa p,boolean presente) {
        Participacao i=new Participacao();i.setPessoa(p);i.setEvento(e);i.setPresente(presente);i.setTipo("Participante");i.setInscricao(Math.toIntExact(p.getId()));return participacoes.saveAndFlush(i);
    }
    String token(Pessoa p) { return "Bearer "+jwt.gerarToken(p); }
    int inscrever(Evento e,Pessoa p) throws Exception {
        return mvc.perform(post("/api/eventos/{id}/inscricoes",e.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new InscricaoPublicaRequest(p.getNome(),p.getCpf(),p.getEmail())))).andReturn().getResponse().getStatus();
    }
    int confirmar(Evento e,Pessoa p) throws Exception {
        return mvc.perform(post("/api/eventos/{id}/confirmar-presenca/{cpf}",e.getId(),p.getCpf())).andReturn().getResponse().getStatus();
    }
    MockMultipartFile assinatura(String formato) {
        return new MockMultipartFile("file","assinatura."+formato,formato.equals("png")?"image/png":"image/jpeg",ImagensTeste.criar(formato));
    }
    ResultActions concluir(Evento e,Long... presentes) throws Exception {
        var req=multipart("/api/admin/eventos/{id}/concluir",e.getId()).file(assinatura("png")).header("Authorization",token(admin));
        for(Long id:presentes)req.param("presentes",id.toString());
        return mvc.perform(req);
    }
    List<Integer> simultaneas(Callable<Integer> a,Callable<Integer> b) throws Exception {
        CountDownLatch prontos=new CountDownLatch(2), inicio=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> tarefas=new ArrayList<>();
            for(var acao:List.of(a,b))tarefas.add(pool.submit(() -> {prontos.countDown();if(!inicio.await(15,TimeUnit.SECONDS))throw new AssertionError("Barreira expirou");return acao.call();}));
            assertThat(prontos.await(15,TimeUnit.SECONDS)).isTrue();inicio.countDown();
            return List.of(tarefas.get(0).get(30,TimeUnit.SECONDS),tarefas.get(1).get(30,TimeUnit.SECONDS));
        }
    }

    @Test void dialetoCorrespondeAoBancoReal() {
        var dialeto = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
        assertThat(dialeto).isInstanceOf("mariadb".equals(System.getProperty("muttley.test.database")) ? MariaDBLegacyDialect.class : MySQLDialect.class);
    }

    @Test void painelExecutaConsultasDePeriodoComLocalDate() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        Participacao p=inscrito(e,ana,true);
        Certificado c=new Certificado();c.setParticipacao(p);c.setDataEmissao(LocalDate.of(2026,9,3));c.setCodigoValidacao("painel-localdate");
        certificados.saveAndFlush(c);
        LocalDate hoje=LocalDate.of(2026,9,3);
        assertThat(eventoService.contarEventosAtivosNoPeriodo(hoje,hoje)).isEqualTo(1);
        assertThat(eventoService.contarEventosAtivosNoPeriodo(hoje.plusDays(1),hoje.plusDays(7))).isZero();
        assertThat(certificadoService.contarEmitidosDesde(hoje)).isEqualTo(1);
        assertThat(certificadoService.contarEmitidosEntre(hoje,hoje.plusDays(1))).isEqualTo(1);
        assertThat(certificadoService.contarEmitidosEntre(hoje.minusDays(1),hoje)).isZero();
        mvc.perform(get("/api/admin/inicio").header("Authorization",token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eventosAtivos").value(1))
                .andExpect(jsonPath("$.proximosEventos[0].disciplina").value("Programação"))
                .andExpect(jsonPath("$.proximosEventos[0].local").value("Auditório"))
                .andExpect(jsonPath("$.proximosEventos[0].participacoes").doesNotExist());
    }

    @Test void respostasSimultaneasDeQrCodePreservamOsDoisLinks() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        simultaneas(() -> {eventoService.salvarQrCodeInscricaoUrl(e.getId(),"inscricao");return 200;},
                () -> {eventoService.salvarQrCodeConfirmacaoUrl(e.getId(),"confirmacao");return 200;});
        Evento salvo=eventos.findById(e.getId()).orElseThrow();
        assertThat(salvo.getQrCodeInscricaoUrl()).isEqualTo("inscricao");
        assertThat(salvo.getQrCodeConfirmacaoUrl()).isEqualTo("confirmacao");
    }

    @ParameterizedTest @CsvSource({"09:59:59,201","10:00:00,400","10:00:01,400"})
    void inscricaoFechaExatamenteNoInicio(String hora,int esperado) throws Exception {
        clock.horario(hora);Evento e=evento(10,StatusEventoEnum.CRIADO,"10:00","11:00");
        assertThat(inscrever(e,ana)).isEqualTo(esperado);
        assertThat(participacoes.count()).isEqualTo(esperado==201?1:0);
    }
    @ParameterizedTest @CsvSource({"09:49:59,409","09:50:00,200","10:00:00,200","11:00:00,200","11:10:00,200","11:10:01,409"})
    void presencaRespeitaAsDuasBordasDaTolerancia(String hora,int esperado) throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"10:00","11:00");var p=inscrito(e,ana,false);clock.horario(hora);
        assertThat(confirmar(e,ana)).isEqualTo(esperado);
        assertThat(participacoes.findById(p.getId()).orElseThrow().isPresente()).isEqualTo(esperado==200);
        assertThat(medalhas.count()).isEqualTo(esperado==200?1:0);
    }
    @ParameterizedTest @EnumSource(value=StatusEventoEnum.class,names={"CANCELADO","FINALIZADO"})
    void presencaRejeitaEventoEncerradoMesmoDentroDaJanela(StatusEventoEnum status) throws Exception {
        Evento e=evento(10,status,"09:00","11:00");inscrito(e,ana,false);
        assertThat(confirmar(e,ana)).isEqualTo(409);assertThat(medalhas.count()).isZero();
    }
    @Test void presencaSemInscricaoECpfInvalidoNaoGeramMedalha() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");
        assertThat(confirmar(e,ana)).isEqualTo(404);
        mvc.perform(post("/api/eventos/{id}/confirmar-presenca/11111111111",e.getId())).andExpect(status().isBadRequest());
        assertThat(medalhas.count()).isZero();
    }
    @Test void ultimaVagaTemUmVencedorComDuasInscricoesSimultaneas() throws Exception {
        Evento e=evento(1,StatusEventoEnum.CRIADO,"11:00","12:00");
        assertThat(simultaneas(() -> inscrever(e,ana),() -> inscrever(e,bia))).containsExactlyInAnyOrder(201,409);
        assertThat(participacoes.countByEventoId(e.getId())).isEqualTo(1);
    }
    @Test void mesmaPessoaNaoSeInscreveDuasVezesSobConcorrencia() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        assertThat(simultaneas(() -> inscrever(e,ana),() -> inscrever(e,ana))).containsExactlyInAnyOrder(201,409);
        assertThat(participacoes.count()).isEqualTo(1);
    }
    @Test void eventosDiferentesRecebemNumerosDistintosSobConcorrencia() throws Exception {
        Evento a=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00"),b=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        assertThat(simultaneas(() -> inscrever(a,ana),() -> inscrever(b,bia))).containsOnly(201);
        assertThat(participacoes.findAll()).extracting(Participacao::getInscricao).doesNotHaveDuplicates().hasSize(2);
    }
    @Test void ultimaVagaNaoPodeSerIgnoradaPeloCrudAutenticado() throws Exception {
        Evento e=evento(1,StatusEventoEnum.CRIADO,"11:00","12:00");inscrito(e,ana,false);
        var dto=new AtualizacaoParticipacao(null,999,"Participante",bia.getId(),e.getId());
        mvc.perform(post("/api/participacoes").header("Authorization",token(bia)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto)))
                .andExpect(status().isConflict());assertThat(participacoes.count()).isEqualTo(1);
    }
    @Test void ultimaVagaTemUmVencedorTambemNoCrudAutenticado() throws Exception {
        Evento e=evento(1,StatusEventoEnum.CRIADO,"11:00","12:00");
        Callable<Integer> cadastrarAna=() -> mvc.perform(post("/api/participacoes")
                .header("Authorization",token(ana)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AtualizacaoParticipacao(null,999,"Participante",ana.getId(),e.getId()))))
                .andReturn().getResponse().getStatus();
        Callable<Integer> cadastrarBia=() -> mvc.perform(post("/api/participacoes")
                .header("Authorization",token(bia)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AtualizacaoParticipacao(null,999,"Participante",bia.getId(),e.getId()))))
                .andReturn().getResponse().getStatus();
        assertThat(simultaneas(cadastrarAna,cadastrarBia)).containsExactlyInAnyOrder(201,409);
        assertThat(participacoes.countByEventoId(e.getId())).isEqualTo(1);
    }
    @Test void duasPresencasSimultaneasGeramSomenteUmBronze() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");inscrito(e,ana,false);
        assertThat(simultaneas(() -> confirmar(e,ana),() -> confirmar(e,ana))).containsExactlyInAnyOrder(200,409);
        assertThat(medalhas.count()).isEqualTo(1);
    }
    @Test void conclusaoEmiteSomenteParaPresentesIgnoraOutroEventoENaoDuplica() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var presente=inscrito(e,ana,false);inscrito(e,bia,false);
        var outro=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),admin,false);
        concluir(e,presente.getId(),outro.getId()).andExpect(status().isOk());
        assertThat(eventos.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(StatusEventoEnum.FINALIZADO);
        assertThat(certificados.count()).isEqualTo(1);assertThat(medalhas.count()).isEqualTo(1);
        assertThat(participacoes.findById(outro.getId()).orElseThrow().isPresente()).isFalse();
        concluir(e,presente.getId()).andExpect(status().isConflict());
        assertThat(certificados.count()).isEqualTo(1);assertThat(medalhas.count()).isEqualTo(1);
        verify(kafka,times(2)).send(eq("email.evento.concluido"),eq(e.getId().toString()),any());
        verify(kafka,times(1)).send(eq("email.certificado"),any(Object.class));
    }
    @Test void duasConclusoesSimultaneasEmitemUmaVez() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var p=inscrito(e,ana,true);
        assertThat(simultaneas(() -> concluir(e,p.getId()).andReturn().getResponse().getStatus(),
                () -> concluir(e,p.getId()).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
        assertThat(certificados.count()).isEqualTo(1);assertThat(medalhas.count()).isEqualTo(1);
    }
    @Test void falhaNaEmissaoDesfazPresencaMedalhaEventoEArquivo() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var p=inscrito(e,ana,false);
        long antes;try(var stream=Files.list(arquivos)){antes=stream.count();}
        doThrow(new IllegalStateException("Falha simulada na emissão")).when(certificadoService).gerarCertificadosParaParticipacoes(anyList(),anyString());
        concluir(e,p.getId()).andExpect(status().isConflict());
        assertThat(participacoes.findById(p.getId()).orElseThrow().isPresente()).isFalse();
        assertThat(medalhas.count()).isZero();assertThat(certificados.count()).isZero();
        assertThat(eventos.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(StatusEventoEnum.EM_ANDAMENTO);
        try(var stream=Files.list(arquivos)){assertThat(stream.count()).isEqualTo(antes);}
        verifyNoInteractions(kafka);
    }
    @Test void emissaoDiretaConcorrenteNaoDuplicaCertificado() throws Exception {
        var p=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),ana,true);
        assertThat(simultaneas(() -> certificadoService.gerarCertificadosParaParticipacoes(List.of(p.getId())).size(),
                () -> certificadoService.gerarCertificadosParaParticipacoes(List.of(p.getId())).size())).containsExactlyInAnyOrder(1,0);
        assertThat(certificados.count()).isEqualTo(1);
    }
    @Test void emissaoDiretaRejeitaAusente() {
        var p=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),ana,false);
        assertThatThrownBy(() -> certificadoService.gerarCertificadosParaParticipacoes(List.of(p.getId())))
                .isInstanceOf(IllegalStateException.class);assertThat(certificados.count()).isZero();
    }
    @Test void bronzeConcorrenteNaoDuplicaMasMedalhasManuaisPodemSerAdicionadas() throws Exception {
        var p=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),ana,true);
        assertThat(simultaneas(() -> medalhaService.gerarMedalhaBronzePorPresenca(p)==null?0:1,
                () -> medalhaService.gerarMedalhaBronzePorPresenca(p)==null?0:1)).containsExactlyInAnyOrder(1,0);
        medalhaService.salvarOuAtualizar(new AtualizacaoMedalha(null,"Extra","Mérito",TipoMedalha.BRONZE,p.getId()));
        assertThat(medalhas.count()).isEqualTo(2);
    }
    @Test void usuarioSoListaLeEAlteraParticipacaoPropria() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");var a=inscrito(e,ana,false);var b=inscrito(e,bia,false);
        mvc.perform(get("/api/participacoes").header("Authorization",token(ana))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(a.getId()));
        mvc.perform(get("/api/participacoes/{id}",b.getId()).header("Authorization",token(ana))).andExpect(status().isForbidden());
        var dto=new AtualizacaoParticipacao(a.getId(),a.getInscricao(),"Participante",ana.getId(),e.getId());
        mvc.perform(put("/api/participacoes/{id}",b.getId()).header("Authorization",token(ana)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isForbidden());
        mvc.perform(put("/api/participacoes/{id}",a.getId()).header("Authorization",token(ana)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isOk());
        mvc.perform(delete("/api/participacoes/{id}",b.getId()).header("Authorization",token(ana))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/participacoes/{id}",a.getId()).header("Authorization",token(ana))).andExpect(status().isOk());
        assertThat(participacoes.findById(b.getId())).isPresent();
    }
    @Test void usuarioNaoPodeCriarOuTransferirParticipacaoParaOutraPessoa() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");var a=inscrito(e,ana,false);
        var dto=new AtualizacaoParticipacao(a.getId(),1,"Participante",bia.getId(),e.getId());
        for(var req:List.of(post("/api/participacoes"),put("/api/participacoes/"+a.getId())))
            mvc.perform(req.header("Authorization",token(ana)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dto))).andExpect(status().isForbidden());
        assertThat(participacoes.count()).isEqualTo(1);
    }
    @Test void administradorPodeGerenciarParticipacaoAlheia() throws Exception {
        var p=inscrito(evento(10,StatusEventoEnum.CRIADO,"11:00","12:00"),ana,false);
        mvc.perform(get("/api/participacoes/{id}",p.getId()).header("Authorization",token(admin))).andExpect(status().isOk());
        mvc.perform(delete("/api/participacoes/{id}",p.getId()).header("Authorization",token(admin))).andExpect(status().isOk());
    }
    @Test void meIsolaParticipacoesCertificadosEMedalhasEntrePessoas() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var a=inscrito(e,ana,true);var b=inscrito(e,bia,true);
        certificadoService.gerarCertificadosParaParticipacoes(List.of(a.getId(),b.getId()));
        medalhaService.gerarMedalhaBronzePorPresenca(a);medalhaService.gerarMedalhaBronzePorPresenca(b);
        for(String recurso:List.of("participacoes","certificados","medalhas")) {
            String body=mvc.perform(get("/api/me/"+recurso).header("Authorization",token(ana))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1)).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain(bia.getEmail());
        }
    }
    @ParameterizedTest @ValueSource(strings={"png","jpg"})
    void assinaturaRealPngOuJpgEIncorporadaAoTemplate(String formato) throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var p=inscrito(e,ana,true);
        mvc.perform(multipart("/api/admin/eventos/{id}/concluir",e.getId()).file(assinatura(formato)).header("Authorization",token(admin))).andExpect(status().isOk());
        Certificado c=certificados.findAll().getFirst();assertThat(Files.readAllBytes(Path.of(c.getCaminhoAssinaturaVisual()))).containsExactly(ImagensTeste.criar(formato));
        when(pdf.gerarPdf(anyString())).thenReturn(new byte[]{37,80,68,70});
        mvc.perform(get("/api/certificados/{codigo}/preview",c.getCodigoValidacao())).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        verify(pdf).gerarPdf(contains("data:"+(formato.equals("png")?"image/png":"image/jpeg")+";base64,"));
    }
    @Test void assinaturaAusenteVaziaOuDisfarcadaNaoAlteraEvento() throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");inscrito(e,ana,true);
        mvc.perform(multipart("/api/admin/eventos/{id}/concluir",e.getId()).header("Authorization",token(admin))).andExpect(status().isBadRequest());
        for(var file:List.of(new MockMultipartFile("file","a.png","image/png",new byte[0]),
                new MockMultipartFile("file","a.png","image/png","nao sou imagem".getBytes()),
                new MockMultipartFile("file","a.gif","image/gif",ImagensTeste.criar("gif")),
                new MockMultipartFile("file","a.jpg","image/jpeg",ImagensTeste.criar("png"))))
            mvc.perform(multipart("/api/admin/eventos/{id}/concluir",e.getId()).file(file).header("Authorization",token(admin))).andExpect(status().isBadRequest());
        assertThat(certificados.count()).isZero();assertThat(medalhas.count()).isZero();
        assertThat(eventos.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(StatusEventoEnum.EM_ANDAMENTO);
    }
    @Test void eventoCanceladoNaoPodeSerEditadoEIdDaUrlPrevalece() throws Exception {
        Evento cancelado=evento(10,StatusEventoEnum.CANCELADO,"11:00","12:00"),aberto=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        var dto=new AtualizacaoEvento(cancelado.getId(),"Tema alterado","Descricao",LocalDate.now().plusDays(1),"11:00","12:00",aberto.getModalidade(),StatusEventoEnum.FINALIZADO,
                aberto.getDisciplina().getId(),aberto.getPatrocinador().getId(),aberto.getLocal().getId());
        String body=json.writeValueAsString(new EventoComParticipacaoDTO(dto,null));
        mvc.perform(put("/api/admin/eventos/{id}",cancelado.getId()).header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/eventos/{id}",aberto.getId()).header("Authorization",token(admin)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        assertThat(eventos.count()).isEqualTo(2);assertThat(eventos.findById(aberto.getId()).orElseThrow().getTema()).isEqualTo("Tema alterado");
        assertThat(eventos.findById(cancelado.getId()).orElseThrow().getTema()).isEqualTo("Semana acadêmica");
    }

    @Test void cadastroLoginPerfilInicialEEmailUnicoPelaApi() throws Exception {
        pessoas.deleteAllInBatch();
        var primeiro=new AtualizacaoPessoa(null,"Primeiro","primeiro@example.invalid","11999999999","529.982.247-25","Senha-forte-123");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "nome",primeiro.nome(),"email",primeiro.email()))))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.senha").doesNotExist());
        assertThat(pessoas.findByEmail(primeiro.email()).orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(pessoas.findByEmail(primeiro.email()).orElseThrow().getSenha()).isNull();
        assertThat(pessoas.findByEmail(primeiro.email()).orElseThrow().getCpf()).isNull();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",primeiro.email(),"senha",primeiro.senha()))))
                .andExpect(status().isUnauthorized());
        var conviteCaptor=org.mockito.ArgumentCaptor.forClass(CadastroEmail.class);
        verify(kafka).send(eq("email.completar.cadastro"),anyString(),conviteCaptor.capture());
        String convite=conviteCaptor.getValue().id();
        var segundo=Map.of("nome","Segundo","email","segundo@example.invalid");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(segundo)))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(segundo))).andExpect(status().isAccepted());
        var todosConvites=org.mockito.ArgumentCaptor.forClass(CadastroEmail.class);
        verify(kafka,times(2)).send(eq("email.completar.cadastro"),anyString(),todosConvites.capture());
        String conviteSegundo=todosConvites.getAllValues().get(1).id();
        mvc.perform(get("/api/pessoa/dados-cadastro/{token}",conviteSegundo)).andExpect(status().isOk());
        mvc.perform(put("/api/auth/register").param("token",convite).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nome",primeiro.nome(),"email",primeiro.email(),
                        "telefone",primeiro.telefone(),"cpf",primeiro.cpf(),"senha",primeiro.senha()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));
        String login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",primeiro.email(),"senha",primeiro.senha()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200)).andReturn().getResponse().getContentAsString();
        String accessToken=json.readTree(login).get("accessToken").asText();
        mvc.perform(get("/api/me").header("Authorization","Bearer "+accessToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(primeiro.email()));
        assertThat(pessoas.count()).isEqualTo(2);
    }

    @Test void inscricaoParcialCompletaCadastroUmaVezEPermiteLoginSemVerificacaoDeEmail() throws Exception {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        var parcial=Map.of("nomeCompleto","Novo participante","email","novo@example.invalid","cpf","390.533.447-05");
        mvc.perform(post("/api/eventos/{id}/inscricoes",e.getId()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(parcial))).andExpect(status().isCreated());
        Pessoa criada=pessoas.findByEmail("novo@example.invalid").orElseThrow();
        assertThat(criada.getSenha()).isNull();assertThat(criada.getRole()).isEqualTo(Role.USER);
        var completo=Map.of("nome","Novo participante","email","novo@example.invalid","cpf","390.533.447-05","telefone","11999999999","senha","Senha-forte-123");
        mvc.perform(put("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(completo))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/auth/register").param("token","convite-invalido").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(completo))).andExpect(status().isNotFound());
        var conviteCaptor=org.mockito.ArgumentCaptor.forClass(CadastroEmail.class);
        verify(kafka).send(eq("email.completar.cadastro"),anyString(),conviteCaptor.capture());
        String convite=conviteCaptor.getValue().id();
        mvc.perform(get("/api/pessoa/dados-cadastro/{token}",convite)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(criada.getEmail()));
        mvc.perform(put("/api/auth/register").param("token",convite).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(completo))).andExpect(status().isOk());
        mvc.perform(put("/api/auth/register").param("token",convite).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(completo))).andExpect(status().isNotFound());
        mvc.perform(get("/api/pessoa/dados-cadastro/{token}",convite)).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email","novo@example.invalid","senha","Senha-forte-123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        verify(kafka).send(eq("email.inscricao.confirmada"),anyString(),any());
        verify(kafka).send(eq("email.completar.cadastro"),anyString(),any());
    }

    @Test void mysqlImpedeParticipacaoDuplicadaMesmoSemPassarPeloServico() {
        Evento e=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");inscrito(e,ana,false);
        assertThatThrownBy(() -> inscrito(e,ana,false)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(participacoes.count()).isEqualTo(1);
    }

    @Test void mysqlImpedeCertificadoDuplicadoPorParticipacao() {
        var p=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),ana,true);
        certificadoService.gerarCertificadosParaParticipacoes(List.of(p.getId()));
        Certificado duplicado=new Certificado();duplicado.setParticipacao(p);duplicado.setDataEmissao(LocalDate.now());
        assertThatThrownBy(() -> certificados.saveAndFlush(duplicado)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(certificados.count()).isEqualTo(1);
    }

    @Test void mysqlImpedeEmailDuplicado() {
        Pessoa duplicada=new Pessoa();duplicada.setNome("Outra pessoa");duplicada.setEmail(ana.getEmail());
        assertThatThrownBy(() -> pessoas.saveAndFlush(duplicada)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(pessoas.count()).isEqualTo(3);
    }

    @Test void cadastroPublicoComIdAlheioNaoSobrescrevePessoaOuSuasParticipacoes() throws Exception {
        var p=inscrito(evento(10,StatusEventoEnum.CRIADO,"11:00","12:00"),ana,false);
        var payload=Map.of("id",ana.getId(),"nome","Nova pessoa","email","nova@example.invalid","telefone","11999999999","cpf","390.533.447-05","senha","Senha-forte-123");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isAccepted());
        assertThat(pessoas.findById(ana.getId()).orElseThrow().getEmail()).isEqualTo(ana.getEmail());
        assertThat(pessoas.findByEmail("nova@example.invalid").orElseThrow().getId()).isNotEqualTo(ana.getId());
        mvc.perform(get("/api/participacoes/{id}",p.getId()).header("Authorization",token(ana))).andExpect(status().isOk());
    }

    @Test void usuarioNaoTransferePresencaConfirmadaParaOutroEvento() throws Exception {
        var p=inscrito(evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00"),ana,true);
        Evento destino=evento(10,StatusEventoEnum.CRIADO,"11:00","12:00");
        var dto=new AtualizacaoParticipacao(p.getId(),p.getInscricao(),"Participante",ana.getId(),destino.getId());
        mvc.perform(put("/api/participacoes/{id}",p.getId()).header("Authorization",token(ana)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(dto))).andExpect(status().isConflict());
        assertThat(participacoes.countByEventoId(destino.getId())).isZero();
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void uploadIndividualEPorEventoValidamImagemEPersistemJpg(boolean porEvento) throws Exception {
        Evento e=evento(10,StatusEventoEnum.EM_ANDAMENTO,"09:00","11:00");var p=inscrito(e,ana,true);
        var c=certificadoService.gerarCertificadosParaParticipacoes(List.of(p.getId())).getFirst();
        String rota=porEvento?"/api/admin/certificados/evento/"+e.getId()+"/upload-assinatura":"/api/admin/certificados/"+c.getId()+"/upload-assinatura";
        mvc.perform(multipart(rota).file(new MockMultipartFile("file","disfarce.png","image/png","texto".getBytes())).header("Authorization",token(admin))).andExpect(status().isBadRequest());
        assertThat(certificados.findById(c.getId()).orElseThrow().getCaminhoAssinaturaVisual()).isNull();
        mvc.perform(multipart(rota).file(assinatura("jpg")).header("Authorization",token(admin))).andExpect(status().isOk());
        var atualizado=certificados.findById(c.getId()).orElseThrow();
        assertThat(atualizado.getCaminhoAssinaturaVisual()).endsWith(".jpg");
        assertThat(Files.readAllBytes(Path.of(atualizado.getCaminhoAssinaturaVisual()))).containsExactly(ImagensTeste.criar("jpg"));
    }
}
