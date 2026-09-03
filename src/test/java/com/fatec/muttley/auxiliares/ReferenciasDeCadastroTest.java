package com.fatec.muttley.auxiliares;

import com.fatec.muttley.local.*;
import com.fatec.muttley.endereco.*;
import com.fatec.muttley.disciplina.*;
import com.fatec.muttley.disciplina.enums.TurnoDisciplinaEnum;
import com.fatec.muttley.professor.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReferenciasDeCadastroTest {
    private LocalService localService(LocalRepository repo,EnderecoService enderecos) {
        var service=new LocalService();ReflectionTestUtils.setField(service,"localRepository",repo);
        ReflectionTestUtils.setField(service,"enderecoService",enderecos);ReflectionTestUtils.setField(service,"localMapper",Mappers.getMapper(LocalMapper.class));return service;
    }
    private DisciplinaService disciplinaService(DisciplinaRepository repo,ProfessorRepository professores) {
        var service=new DisciplinaService();ReflectionTestUtils.setField(service,"disciplinaRepository",repo);
        ReflectionTestUtils.setField(service,"professorRepository",professores);ReflectionTestUtils.setField(service,"disciplinaMapper",Mappers.getMapper(DisciplinaMapper.class));return service;
    }
    @Test void localExigeEnderecoExistente() {
        var repo=mock(LocalRepository.class);var enderecos=mock(EnderecoService.class);
        assertThatThrownBy(() -> localService(repo,enderecos).salvarOuAtualizar(new AtualizacaoLocal(null,"Auditorio","Descricao",100,1L)))
                .isInstanceOf(EntityNotFoundException.class);verifyNoInteractions(repo);
    }
    @Test void localPersisteReferenciaResolvidaECapacidade() {
        var repo=mock(LocalRepository.class);var enderecos=mock(EnderecoService.class);var endereco=new Endereco();
        when(enderecos.procurarPorId(1L)).thenReturn(Optional.of(endereco));when(repo.save(any())).thenAnswer(i->i.getArgument(0));
        var local=localService(repo,enderecos).salvarOuAtualizar(new AtualizacaoLocal(null,"Auditorio","Descricao",100,1L));
        assertThat(local.getEndereco()).isSameAs(endereco);assertThat(local.getCapacidade()).isEqualTo(100);assertThat(local.getNome()).isEqualTo("Auditorio");
    }
    @Test void disciplinaAceitaProfessorOpcional() {
        var repo=mock(DisciplinaRepository.class);var professores=mock(ProfessorRepository.class);when(repo.save(any())).thenAnswer(i->i.getArgument(0));
        var d=disciplinaService(repo,professores).salvarOuAtualizar(new AtualizacaoDisciplina(null,"Programacao","Descricao",TurnoDisciplinaEnum.values()[0],null));
        assertThat(d.getProfessor()).isNull();assertThat(d.getNome()).isEqualTo("Programacao");verifyNoInteractions(professores);
    }
    @Test void disciplinaRejeitaProfessorInformadoInexistente() {
        var repo=mock(DisciplinaRepository.class);var professores=mock(ProfessorRepository.class);
        assertThatThrownBy(() -> disciplinaService(repo,professores).salvarOuAtualizar(new AtualizacaoDisciplina(null,"Programacao","Descricao",TurnoDisciplinaEnum.values()[0],1L)))
                .isInstanceOf(EntityNotFoundException.class);verifyNoInteractions(repo);
    }
    @Test void disciplinaVinculaProfessorResolvido() {
        var repo=mock(DisciplinaRepository.class);var professores=mock(ProfessorRepository.class);var professor=new Professor();
        when(professores.findById(1L)).thenReturn(Optional.of(professor));when(repo.save(any())).thenAnswer(i->i.getArgument(0));
        var d=disciplinaService(repo,professores).salvarOuAtualizar(new AtualizacaoDisciplina(null,"Programacao","Descricao",TurnoDisciplinaEnum.values()[0],1L));
        assertThat(d.getProfessor()).isSameAs(professor);
    }
}
