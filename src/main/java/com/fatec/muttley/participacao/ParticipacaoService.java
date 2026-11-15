package com.fatec.muttley.participacao;

import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.evento.EventoService;
import com.fatec.muttley.evento.HorariosEvento;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.pessoa.Pessoa;
import com.fatec.muttley.pessoa.PessoaService;
import com.fatec.muttley.pessoa.Role;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ParticipacaoService {
    /** Tipo da participação de quem apresenta o evento; também dá a medalha especial Palestrante. */
    public static final String TIPO_PALESTRANTE = "Palestrante";

    private final Clock clock;

    private final NumeroInscricaoService numeros;

    private final ParticipacaoRepository participacaoRepository;

    private final ParticipacaoMapper participacaoMapper;

    private final PessoaService pessoaService;

    private final EventoService eventoService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Participacao salvarOuAtualizar(AtualizacaoParticipacao dto) {
        Pessoa pessoa = pessoaService.procurarPorId(dto.pessoaId())
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada com o id: " + dto.pessoaId()));
        Evento evento = eventoService.procurarPorIdParaAtualizacao(dto.eventoId())
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado com o id: " + dto.eventoId()));

        if (dto.id() != null) {
            Participacao existente = participacaoRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada com o id: " + dto.id()));
            if (!existente.getEvento().getId().equals(evento.getId())) {
                if (existente.isPresente()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Uma presença confirmada não pode ser transferida para outro evento.");
                }
                validarInscricaoAberta(evento);
                validarVagas(evento);
            }
            if ((!existente.getPessoa().getId().equals(pessoa.getId()) || !existente.getEvento().getId().equals(evento.getId()))
                    && participacaoRepository.existsByEventoIdAndPessoaId(evento.getId(), pessoa.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Pessoa ja inscrita neste evento.");
            }
            participacaoMapper.updateEntityFromDto(dto, existente);
            existente.setPessoa(pessoa);
            existente.setEvento(evento);
            return participacaoRepository.save(existente);
        } else {
            validarInscricaoAberta(evento);
            validarVagas(evento);
            if (participacaoRepository.existsByEventoIdAndPessoaId(evento.getId(), pessoa.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Pessoa ja inscrita neste evento.");
            }
            Participacao novoParticipacao = participacaoMapper.toEntityFromAtualizacao(dto);
            novoParticipacao.setInscricao(numeros.proximo());
            novoParticipacao.setPessoa(pessoa);
            novoParticipacao.setEvento(evento);
            return participacaoRepository.save(novoParticipacao);
        }
    }

    /**
     * Deixa os palestrantes do evento iguais à lista: cria quem entrou, promove
     * quem já estava inscrito e remove quem saiu. Palestrante não depende de
     * inscrições abertas nem ocupa vaga da plateia.
     */
    @Transactional
    public void definirPalestrantes(Long eventoId, List<Long> pessoaIds) {
        Evento evento = eventoService.procurarPorIdParaAtualizacao(eventoId)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado com o id: " + eventoId));
        Set<Long> desejados = new LinkedHashSet<>(pessoaIds);

        for (Participacao participacao : participacaoRepository.findByEventoIdComDadosOrderByInscricaoAsc(eventoId)) {
            boolean palestrante = TIPO_PALESTRANTE.equalsIgnoreCase(participacao.getTipo());
            if (desejados.remove(participacao.getPessoa().getId())) {
                participacao.setTipo(TIPO_PALESTRANTE);
            } else if (palestrante) {
                if (participacao.isPresente()) {
                    throw new IllegalStateException(participacao.getPessoa().getNome()
                            + " já teve a presença registrada e não pode sair dos palestrantes.");
                }
                participacaoRepository.delete(participacao);
            }
        }

        for (Long pessoaId : desejados) {
            Pessoa pessoa = pessoaService.procurarPorId(pessoaId)
                    .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada com o id: " + pessoaId));
            Participacao nova = new Participacao();
            nova.setInscricao(numeros.proximo());
            nova.setTipo(TIPO_PALESTRANTE);
            nova.setPessoa(pessoa);
            nova.setEvento(evento);
            participacaoRepository.save(nova);
        }
    }

    public List<Participacao> procurarPalestrantes(Long eventoId) {
        return participacaoRepository.findByEventoIdComDadosOrderByInscricaoAsc(eventoId).stream()
                .filter(p -> TIPO_PALESTRANTE.equalsIgnoreCase(p.getTipo()))
                .toList();
    }

    public List<Participacao> procurarTodos() {
        return participacaoRepository.findAll(Sort.by("inscricao").ascending());
    }

    public List<Participacao> procurarPorEvento(Long eventoId) {
        return participacaoRepository.findByEventoIdComDadosOrderByInscricaoAsc(eventoId);
    }

    public List<Participacao> procurarPorPessoa(Long pessoaId) {
        return participacaoRepository.findByPessoaIdComDados(pessoaId);
    }

    @Transactional
    public Participacao registrarInscricaoPublica(Long eventoId, InscricaoPublicaRequest dados) {
        Evento evento = eventoService.procurarPorIdParaAtualizacao(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento nao encontrado."));
        validarInscricaoAberta(evento);
        validarVagas(evento);

        Pessoa pessoa = resolverPessoa(dados);
        if (participacaoRepository.existsByEventoIdAndPessoaId(eventoId, pessoa.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pessoa ja inscrita neste evento.");
        }

        Participacao participacao = new Participacao();
        participacao.setInscricao(numeros.proximo());
        participacao.setTipo("Participante");
        participacao.setPessoa(pessoa);
        participacao.setEvento(evento);
        return participacaoRepository.save(participacao);
    }

    public void apagarPorId(Long id) {
        participacaoRepository.deleteById(id);
    }

    /** Desistir só faz sentido antes de o evento começar e enquanto não há presença registrada. */
    public void validarCancelamento(Participacao participacao) {
        if (participacao.isPresente()) {
            throw new IllegalStateException("A presença já foi confirmada; a inscrição não pode mais ser cancelada.");
        }
        Evento evento = participacao.getEvento();
        if (evento.getStatus() != StatusEventoEnum.CRIADO || HorariosEvento.jaIniciou(evento, clock)) {
            throw new IllegalStateException("O evento já começou ou foi encerrado; a inscrição não pode mais ser cancelada.");
        }
    }

    public Optional<Participacao> procurarPorId(Long id) {
        return participacaoRepository.findById(id);
    }

    @Transactional
    public Optional<Participacao> procurarPorIdParaAtualizacao(Long id) {
        return participacaoRepository.findByIdParaAtualizacao(id);
    }

    public Optional<Participacao> procurarPorIdComDados(Long id) {
        return participacaoRepository.findByIdComDados(id);
    }

    @Transactional
    public Participacao confirmarPresenca(Long eventoId, String cpf) {
        Evento evento = eventoService.procurarPorIdParaAtualizacao(eventoId)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado."));
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime inicio = LocalDateTime.of(evento.getData(), LocalTime.parse(evento.getHorarioInicio())).minusMinutes(10);
        LocalDateTime fim = LocalDateTime.of(evento.getData(), LocalTime.parse(evento.getHorarioFim())).plusMinutes(10);
        if (evento.getStatus() == StatusEventoEnum.CANCELADO || evento.getStatus() == StatusEventoEnum.FINALIZADO
                || agora.isBefore(inicio) || agora.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmação de presença fora do período permitido.");
        }
        Pessoa pessoa = pessoaService.procurarPorCpf(cpf)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada com o cpf: " + cpf));

        if(!participacaoRepository.existsByEventoIdAndPessoaId(eventoId, pessoa.getId())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Você não está inscrito nesse evento.");
        }

        Participacao participacao = participacaoRepository.findByEventoIdAndPessoaId(eventoId, pessoa.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Você não está inscrito nesse evento."));

        if (participacao.isPresente()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Presença já confirmada anteriormente.");
        }

        participacao.setPresente(true);

        return participacaoRepository.save(participacao);
    }

    @Transactional
    public Participacao marcarPresente(Long participacaoId) {
        Participacao participacao = participacaoRepository.findById(participacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Participacao nao encontrada com o id: " + participacaoId));
        participacao.setPresente(true);
        return participacaoRepository.save(participacao);
    }

    private Pessoa resolverPessoa(InscricaoPublicaRequest dados) {
        String cpf = normalizar(dados.cpf());
        String email = normalizar(dados.email()).toLowerCase();
        String nome = normalizar(dados.nomeCompleto());

        Optional<Pessoa> pessoaPorCpf = pessoaService.procurarPorCpf(cpf);
        Optional<Pessoa> pessoaPorEmail = pessoaService.procurarPorEmail(email);

        if (pessoaPorCpf.isPresent() && pessoaPorEmail.isPresent()
                && !pessoaPorCpf.get().getId().equals(pessoaPorEmail.get().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF e email pertencem a pessoas diferentes.");
        }

        Pessoa pessoa = pessoaPorCpf.or(() -> pessoaPorEmail).orElseGet(Pessoa::new);
        if (pessoa.getNome() == null || pessoa.getNome().isBlank()) {
            pessoa.setNome(nome);
        }
        if (pessoa.getCpf() == null || pessoa.getCpf().isBlank()) {
            pessoa.setCpf(cpf);
        }
        if (pessoa.getEmail() == null || pessoa.getEmail().isBlank()) {
            pessoa.setEmail(email);
        }
        if (pessoa.getRole() == null) {
            pessoa.setRole(Role.USER);
        }
        return pessoaService.salvar(pessoa);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void validarInscricaoAberta(Evento evento) {
        if (evento.getStatus() != StatusEventoEnum.CRIADO || HorariosEvento.jaIniciou(evento, clock)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inscricoes encerradas para este evento.");
        }
    }

    private void validarVagas(Evento evento) {
        if (evento.getLocal() == null || participacaoRepository.contarPlateia(evento.getId(), TIPO_PALESTRANTE) >= evento.getLocal().getCapacidade()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As vagas deste evento estão esgotadas.");
        }
    }

}
