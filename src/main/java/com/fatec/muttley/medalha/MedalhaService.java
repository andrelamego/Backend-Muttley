package com.fatec.muttley.medalha;

import com.fatec.muttley.participacao.Participacao;
import com.fatec.muttley.participacao.ParticipacaoService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedalhaService {
    private final MedalhaRepository medalhaRepository;

    private final ParticipacaoService participacaoService;

    private final MedalhaMapper medalhaMapper;

    public Medalha salvarOuAtualizar(AtualizacaoMedalha dto) {
        Participacao participacao = participacaoService.procurarPorId(dto.participacaoId())
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada com ID: " + dto.participacaoId()));
        if (dto.id() != null) {
            Medalha existente = medalhaRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Medalha não encontrada com ID: " + dto.id()));
            medalhaMapper.updateEntityFromDto(dto, existente);
            existente.setParticipacao(participacao);
            return medalhaRepository.save(existente);
        } else {
            Medalha novoMedalha = medalhaMapper.toEntityFromAtualizacao(dto);
            novoMedalha.setParticipacao(participacao);

            return medalhaRepository.save(novoMedalha);
        }
    }

    public List<Medalha> procurarTodos(){
        return medalhaRepository.findAll(Sort.by("nome").ascending());
    }

    public List<Medalha> procurarPorPessoa(Long pessoaId) {
        return medalhaRepository.findByPessoaIdComDados(pessoaId);
    }

    public void apagarPorId (Long id) {
        medalhaRepository.deleteById(id);
    }

    public Optional<Medalha> procurarPorId(Long id) {
        return medalhaRepository.findById(id);
    }
}
