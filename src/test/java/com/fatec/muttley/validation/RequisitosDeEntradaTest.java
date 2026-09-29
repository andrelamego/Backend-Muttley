package com.fatec.muttley.validation;

import com.fatec.muttley.aluno.*;
import com.fatec.muttley.certificado.*;
import com.fatec.muttley.colaborador.*;
import com.fatec.muttley.disciplina.*;
import com.fatec.muttley.disciplina.enums.*;
import com.fatec.muttley.endereco.*;
import com.fatec.muttley.evento.*;
import com.fatec.muttley.local.*;
import com.fatec.muttley.medalha.*;
import com.fatec.muttley.organizador.*;
import com.fatec.muttley.palestrante.*;
import com.fatec.muttley.participacao.*;
import com.fatec.muttley.patrocinador.*;
import com.fatec.muttley.pessoa.*;
import com.fatec.muttley.professor.*;
import io.github.andrelamego.brValidator.cpf.CpfValidationService;
import jakarta.validation.*;
import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;

class RequisitosDeEntradaTest {
    static ValidatorFactory factory;
    static Validator validator;
    static AnnotationConfigApplicationContext context;
    @BeforeAll static void iniciar() {
        context=new AnnotationConfigApplicationContext(
                CpfValidationService.class);
        factory=Validation.byDefaultProvider().configure()
                .constraintValidatorFactory(new SpringConstraintValidatorFactory(context.getAutowireCapableBeanFactory()))
                .clockProvider(() -> Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"),ZoneOffset.UTC)).buildValidatorFactory();
        validator=factory.getValidator();
    }
    @AfterAll static void fechar() { factory.close(); context.close(); }
    static Stream<Arguments> obrigatorios() {
        return Stream.of(
                Arguments.of(new AtualizacaoPessoa(null,null,null,null,null,null),"nome,email,telefone,cpf,senha"),
                Arguments.of(new AtualizacaoEvento(null,null,null,null,null,null,null,null,null,null,null),"tema,descricao,data,horarioInicio,horarioFim,modalidade,disciplinaId,patrocinadorId,localId"),
                Arguments.of(new InscricaoPublicaRequest(null,null,null),"nomeCompleto,cpf,email"),
                Arguments.of(new AtualizacaoParticipacao(null,0,null,null,null),"tipo,pessoaId,eventoId"),
                Arguments.of(new AtualizacaoCertificado(null,null,null,null,null),"dataEmissao,assinatura,participacaoId,caminhoAssinaturaVisual"),
                Arguments.of(new AtualizacaoMedalha(null,null,null,null,null),"nome,descricao,tipo,participacaoId"),
                Arguments.of(new AtualizacaoEndereco(null,null,null,null,null,0,null),"estado,cidade,bairro,logradouro,complemento"),
                Arguments.of(new AtualizacaoLocal(null,null,null,0,null),"nome,descricao,enderecoId"),
                Arguments.of(new AtualizacaoDisciplina(null,null,null,null,null),"nome,descricao,turno"),
                Arguments.of(new AtualizacaoPatrocinador(null,null,null,null,null,null,null),"nome,cnpj,valorPatrocinio,email,telefone,site"),
                Arguments.of(new AtualizacaoAluno(null,null,null),"instituicao,matricula"),
                Arguments.of(new AtualizacaoProfessor(null,null,null),"areaFormacao,titulacao"),
                Arguments.of(new AtualizacaoPalestrante(null,null,null,null),"resumoProfissional,empresaAtual,cargo"),
                Arguments.of(new AtualizacaoOrganizador(null,null,null),"instituicao,cargo"),
                Arguments.of(new AtualizacaoColaborador(null,null,null,null),"funcao,disponibilidade,tipo")
        );
    }
    @ParameterizedTest(name="{index}: campos obrigatorios de {0}") @MethodSource("obrigatorios")
    void rejeitaAusenciaDosCamposDescritosNosRequisitos(Object dto,String campos) {
        assertThat(validator.validate(dto).stream().map(v->v.getPropertyPath().toString()).distinct())
                .containsExactlyInAnyOrder(campos.split(","));
    }
    @Test void RN_PES_01_cadastroValidoEEmailECpfInvalidos() {
        assertThat(validator.validate(dadosPessoa(null,"senha"))).isEmpty();
        var invalido=new AtualizacaoPessoa(null,"Nome","nao-e-email","11999999999","111.111.111-11","senha");
        assertThat(validator.validate(invalido)).extracting(v->v.getPropertyPath().toString()).contains("email","cpf");
    }
    @ParameterizedTest @ValueSource(strings={"2026-09-03","2026-09-04"})
    void RN_EVT_02_aceitaHojeEFuturo(String data) {
        assertThat(validator.validateValue(AtualizacaoEvento.class,"data",LocalDate.parse(data))).isEmpty();
    }
    @Test void RN_EVT_02_rejeitaDataPassada() {
        assertThat(validator.validateValue(AtualizacaoEvento.class,"data",LocalDate.of(2026,9,2))).isNotEmpty();
    }
    @Test void RN_CER_07_emissaoManualNaoPodeSerFutura() {
        assertThat(validator.validateValue(AtualizacaoCertificado.class,"dataEmissao",LocalDate.of(2026,9,4))).isNotEmpty();
        assertThat(validator.validateValue(AtualizacaoCertificado.class,"dataEmissao",LocalDate.of(2026,9,3))).isEmpty();
        assertThat(validator.validateValue(AtualizacaoCertificado.class,"dataEmissao",LocalDate.of(2026,9,2))).isEmpty();
    }
    @Test void disciplinaComTurnoValidoEProfessorOpcional() {
        assertThat(validator.validate(new AtualizacaoDisciplina(null,"Programacao","Descricao",TurnoDisciplinaEnum.values()[0],null))).isEmpty();
    }
}
