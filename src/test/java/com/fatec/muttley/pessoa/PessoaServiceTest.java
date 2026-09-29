package com.fatec.muttley.pessoa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {
    @Mock PessoaRepository repository;
    @Spy PessoaMapper mapper=Mappers.getMapper(PessoaMapper.class);
    @Spy BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    @InjectMocks PessoaService service;
    @Test void RN_AUT_02_RN_AUT_05_novoUsuarioRecebeBcryptEPerfilUser() {
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        Pessoa p=service.salvarOuAtualizar(dadosPessoa(null,"Senha-forte-123"));
        assertThat(p.getSenha()).startsWith("$2").isNotEqualTo("Senha-forte-123");
        assertThat(encoder.matches("Senha-forte-123",p.getSenha())).isTrue();
        assertThat(p.getRole()).isEqualTo(Role.USER);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"   "})
    void atualizacaoSemSenhaPreservaHashEPerfil(String senha) {
        Pessoa p=pessoa(1); p.setSenha("hash-existente"); p.setRole(Role.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(p)); when(repository.save(p)).thenReturn(p);
        service.salvarOuAtualizar(dadosPessoa(1L,senha));
        assertThat(p.getSenha()).isEqualTo("hash-existente"); assertThat(p.getRole()).isEqualTo(Role.ADMIN);
        verify(encoder,never()).encode(any());
    }
    @Test void atualizacaoComSenhaGeraNovoHash() {
        Pessoa p=pessoa(1); p.setSenha("hash-anterior");
        when(repository.findById(1L)).thenReturn(Optional.of(p)); when(repository.save(p)).thenReturn(p);
        service.salvarOuAtualizar(dadosPessoa(1L,"nova-senha"));
        assertThat(encoder.matches("nova-senha",p.getSenha())).isTrue();
    }
    @Test void RN_AUT_03_dtoDeRespostaNaoCarregaHash() {
        Pessoa p=pessoa(1); p.setSenha("hash-confidencial");
        assertThat(mapper.toAtualizacaoDto(p).senha()).isNull();
    }
    @Test void RN_AUT_03_serializacaoOmiteSenhaMasPermiteEntrada() throws Exception {
        ObjectMapper json=new ObjectMapper();
        var node=json.readTree(json.writeValueAsString(dadosPessoa(1L,"segredo")));
        assertThat(node.has("senha")).isFalse();
        assertThat(node.get("nome").asText()).isEqualTo("Pessoa de teste");
        assertThat(json.readValue("{\"senha\":\"nova-senha\"}",AtualizacaoPessoa.class).senha()).isEqualTo("nova-senha");
    }
    @Test void RN_AUT_03_entidadeNaoSerializaHash() throws Exception {
        Pessoa p=pessoa(1); p.setSenha("hash-confidencial");
        assertThat(new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(p)).has("senha")).isFalse();
    }
}
