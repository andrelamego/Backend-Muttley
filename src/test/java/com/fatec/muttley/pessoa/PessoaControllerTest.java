package com.fatec.muttley.pessoa;

import com.fatec.muttley.aluno.*;
import com.fatec.muttley.colaborador.*;
import com.fatec.muttley.exceptions.GlobalExceptionHandler;
import com.fatec.muttley.organizador.*;
import com.fatec.muttley.palestrante.*;
import com.fatec.muttley.professor.*;
import java.util.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PessoaControllerTest {
    @Mock PessoaService pessoas; @Mock AlunoService alunos; @Mock ProfessorService professores;
    @Mock PalestranteService palestrantes; @Mock OrganizadorService organizadores; @Mock ColaboradorService colaboradores;
    @Spy PessoaMapper mapper=Mappers.getMapper(PessoaMapper.class);
    @InjectMocks PessoaController controller;
    @Test void RF_PES_01_criaEListaPessoas() {
        var p=pessoa(1);var dto=dadosPessoa(null,"senha");when(pessoas.salvarOuAtualizar(dto)).thenReturn(p);
        when(pessoas.procurarTodos()).thenReturn(List.of(p));
        assertThat(controller.criar(dto).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.listarTodos().getBody()).containsExactly(p);
    }
    @Test void RF_PES_01_atualizaPessoaIndicadaNaUrl() {
        var p=pessoa(1);var dto=dadosPessoa(99L,"senha");when(pessoas.procurarPorId(1L)).thenReturn(Optional.of(p));
        when(pessoas.salvarOuAtualizar(dto.withId(1L))).thenReturn(p);
        assertThat(controller.atualizar(1L,dto).getStatusCode().value()).isEqualTo(200);
        verify(pessoas).salvarOuAtualizar(dto.withId(1L));
    }
    @Test void RF_PES_01_excluiPessoaExistente() {
        when(pessoas.procurarPorId(1L)).thenReturn(Optional.of(pessoa(1)));
        assertThat(controller.deletar(1L).getStatusCode().value()).isEqualTo(200);verify(pessoas).apagarPorId(1L);
    }
    @Test void RF_PES_02_listagensUsamPerfisCorrespondentes() {
        var aluno=new Aluno();var professor=new Professor();var palestrante=new Palestrante();var organizador=new Organizador();var colaborador=new Colaborador();
        when(alunos.procurarTodos()).thenReturn(List.of(aluno));when(professores.procurarTodos()).thenReturn(List.of(professor));
        when(palestrantes.procurarTodos()).thenReturn(List.of(palestrante));when(organizadores.procurarTodos()).thenReturn(List.of(organizador));when(colaboradores.procurarTodos()).thenReturn(List.of(colaborador));
        assertThat(controller.listarAlunos().getBody()).isEqualTo(List.of(aluno));assertThat(controller.listarProfessores().getBody()).isEqualTo(List.of(professor));
        assertThat(controller.listarPalestrantes().getBody()).isEqualTo(List.of(palestrante));assertThat(controller.listarOrganizadores().getBody()).isEqualTo(List.of(organizador));assertThat(controller.listarColaboradores().getBody()).isEqualTo(List.of(colaborador));
    }
    @Test void RN_PES_02_pessoaPodeTerMultiplosPerfisSemPerderOsAnteriores() {
        var p=pessoa(1);p.setAluno(new Aluno());p.setProfessor(new Professor());p.setPalestrante(new Palestrante());p.setOrganizador(new Organizador());p.setColaborador(new Colaborador());
        mapper.updateEntityFromDto(dadosPessoa(1L,"senha"),p);
        assertThat(p.isAluno()).isTrue();assertThat(p.isProfessor()).isTrue();assertThat(p.isPalestrante()).isTrue();assertThat(p.isOrganizador()).isTrue();assertThat(p.isColaborador()).isTrue();
    }
    @Test void RN_AUT_03_respostaHttpDaPessoaNaoExibeSenha() throws Exception {
        var p=pessoa(1);p.setSenha("hash-confidencial");when(pessoas.procurarPorId(1L)).thenReturn(Optional.of(p));
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/api/admin/pessoas/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(p.getNome())).andExpect(jsonPath("$.senha").doesNotExist());
    }
    @Test void recursoInexistenteRetorna404ComMensagem() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/admin/pessoas/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.erro").value("Pessoa não encontrada."));
    }
    @Test void dadosObrigatoriosInvalidosRetornam400ComErrosDeCampo() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(post("/api/admin/pessoas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros").isArray())
                .andExpect(jsonPath("$.erros",Matchers.hasItems(
                        Matchers.startsWith("nome:"),Matchers.startsWith("email:"),
                        Matchers.startsWith("telefone:"),Matchers.startsWith("cpf:"),
                        Matchers.startsWith("senha:"))));
        verifyNoInteractions(pessoas);
    }
}
