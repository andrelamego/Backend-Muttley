package com.fatec.muttley.palestrante;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class PalestranteService {
    private final PalestranteRepository palestranteRepository;

    private final PalestranteMapper palestranteMapper;

    public Palestrante salvarOuAtualizar(AtualizacaoPalestrante dto){
        if (dto.id() != null){
            Palestrante existente = palestranteRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Palestrante não encontrado com id: ." + dto.id()));
            palestranteMapper.updateEntityFromDto(dto, existente);
            return  palestranteRepository.save(existente);
        } else {
            Palestrante novoPalestrante = palestranteMapper.toEntityFromAtualizacao(dto);
            return palestranteRepository.save(novoPalestrante);
        }
    }

    public List<Palestrante> procurarTodos(){
        return palestranteRepository.findAll(Sort.by("cargo").ascending());
    }

    public void apagarPorId(Long id){
        palestranteRepository.deleteById(id);
    }

    public Optional<Palestrante> procurarPorId(Long id){
        return palestranteRepository.findById(id);
    }
}
