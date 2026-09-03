package com.fatec.muttley.medalha;

import com.fatec.muttley.participacao.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static com.fatec.muttley.support.Cenarios.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedalhaServiceTest {
    @Mock MedalhaRepository repository;
    @Mock ParticipacaoService participacoes;
    @Spy MedalhaMapper mapper=Mappers.getMapper(MedalhaMapper.class);
    @InjectMocks MedalhaService service;
    @Test void RF_MED_03_RN_MED_03_04_geraBronzeParaPresente() {
        Participacao p=participacao(1,true); when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(p));
        Medalha medalha=service.gerarMedalhaBronzePorPresenca(p);
        assertThat(medalha.getTipo()).isEqualTo(TipoMedalha.BRONZE);
        assertThat(medalha.getNome()).isNotBlank(); assertThat(medalha.getDescricao()).isNotBlank();
        assertThat(medalha.getParticipacao()).isSameAs(p); verify(repository).save(medalha);
    }
    @ParameterizedTest @ValueSource(strings={"nula","semId","ausente"})
    void RN_MED_04_rejeitaParticipacaoNaoConfirmada(String caso) {
        Participacao p=caso.equals("nula")?null:participacao(1,!caso.equals("ausente"));
        if(caso.equals("semId"))p.setId(null);
        assertThatThrownBy(() -> service.gerarMedalhaBronzePorPresenca(p)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }
    @Test void RN_MED_05_naoDuplicaBronzeAutomatico() {
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(participacao(1,true)));
        when(repository.existsByParticipacaoIdAndTipo(1L,TipoMedalha.BRONZE)).thenReturn(true);
        assertThat(service.gerarMedalhaBronzePorPresenca(participacao(1,true))).isNull();
        verify(repository,never()).save(any());
    }
    @Test void loteIgnoraAusentes() {
        when(participacoes.procurarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(participacao(1,true)));
        service.gerarMedalhasBronzePorPresenca(List.of(participacao(1,true),participacao(2,false)));
        verify(repository).save(argThat(m -> m.getParticipacao().getId().equals(1L)));
        verify(repository,never()).existsByParticipacaoIdAndTipo(2L,TipoMedalha.BRONZE);
    }
    @ParameterizedTest @EnumSource(TipoMedalha.class)
    void RN_MED_02_06_administradorPodeConcederTiposAdicionais(TipoMedalha tipo) {
        when(participacoes.procurarPorId(1L)).thenReturn(Optional.of(participacao(1,false)));
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        Medalha m=service.salvarOuAtualizar(new AtualizacaoMedalha(null,"Destaque","Reconhecimento",tipo,1L));
        assertThat(m.getTipo()).isEqualTo(tipo); assertThat(m.getParticipacao().getId()).isEqualTo(1L);
        verify(repository,never()).existsByParticipacaoIdAndTipo(anyLong(),any());
    }
    @Test void RN_MED_01_exigeParticipacaoExistente() {
        assertThatThrownBy(() -> service.salvarOuAtualizar(new AtualizacaoMedalha(null,"Nome","Descricao",TipoMedalha.OURO,1L)))
                .isInstanceOf(EntityNotFoundException.class); verifyNoInteractions(repository);
    }
    @Test void RN_MED_03_tipoPadraoNoCicloDePersistenciaPreservaEscolhaExplicita() {
        Medalha medalha=new Medalha();medalha.preencherTipoPadrao();assertThat(medalha.getTipo()).isEqualTo(TipoMedalha.BRONZE);
        medalha.setTipo(TipoMedalha.OURO);medalha.preencherTipoPadrao();assertThat(medalha.getTipo()).isEqualTo(TipoMedalha.OURO);
    }
    @Test void RF_MED_01_editaMedalhaPreservandoIdComParticipacaoResolvida() {
        var p=participacao(1,true);var medalha=new Medalha();medalha.setId(7L);
        when(participacoes.procurarPorId(1L)).thenReturn(Optional.of(p));when(repository.findById(7L)).thenReturn(Optional.of(medalha));when(repository.save(medalha)).thenReturn(medalha);
        var atualizada=service.salvarOuAtualizar(new AtualizacaoMedalha(7L,"Destaque","Descricao",TipoMedalha.PRATA,1L));
        assertThat(atualizada.getId()).isEqualTo(7L);assertThat(atualizada.getTipo()).isEqualTo(TipoMedalha.PRATA);assertThat(atualizada.getParticipacao()).isSameAs(p);
    }
}
