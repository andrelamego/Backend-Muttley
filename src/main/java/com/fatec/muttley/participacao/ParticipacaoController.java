package com.fatec.muttley.participacao;

import com.fatec.muttley.email.EmailProducer;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participacoes")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ParticipacaoController {
    @Autowired
    private ParticipacaoAcessoService acesso;

    @Autowired
    private ParticipacaoService participacaoService;

    @Autowired
    private ParticipacaoMapper participacaoMapper;

    @Autowired
    private EmailProducer emailProducer;

    @GetMapping
    public ResponseEntity<List<ParticipacaoComEventoResponse>> listarTodos() {
        List<ParticipacaoComEventoResponse> participacoes = acesso.listar().stream()
                .map(ParticipacaoComEventoResponse::from)
                .toList();
        return ResponseEntity.ok(participacoes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipacaoComEventoResponse> buscarPorId(@PathVariable Long id) {
        Participacao participacao = participacaoService.procurarPorIdComDados(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(participacao);
        return ResponseEntity.ok(ParticipacaoComEventoResponse.from(participacao));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoParticipacao dto) {
        acesso.validarPessoa(dto.pessoaId());
        Participacao participacaoSalva = participacaoService.salvarOuAtualizar(dto.withId(null));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Participação '" + participacaoSalva.getInscricao() + "' criada com sucesso."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> atualizar(@PathVariable Long id,
                                                         @RequestBody @Valid AtualizacaoParticipacao dto) {
        Participacao existente = participacaoService.procurarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(existente);
        acesso.validarPessoa(dto.pessoaId());
        dto = dto.withId(id);
        Participacao participacaoSalva = participacaoService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Participação '" + participacaoSalva.getInscricao() + "' alterada com sucesso."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletar(@PathVariable Long id) {
        Participacao existente = participacaoService.procurarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada."));
        acesso.validarParticipacao(existente);
        participacaoService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Participação " + id + " deletada com sucesso."));
    }
}
