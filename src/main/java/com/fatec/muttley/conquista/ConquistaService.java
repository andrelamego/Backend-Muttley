package com.fatec.muttley.conquista;

import com.fatec.muttley.conquista.Conquistas.Destaque;
import com.fatec.muttley.conquista.Conquistas.Disputa;
import com.fatec.muttley.conquista.Conquistas.Especial;
import com.fatec.muttley.conquista.Conquistas.Presenca;
import com.fatec.muttley.conquista.Conquistas.Trilha;
import com.fatec.muttley.evento.Evento;
import com.fatec.muttley.evento.enums.StatusEventoEnum;
import com.fatec.muttley.participacao.ParticipacaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConquistaService {
    private final ParticipacaoRepository participacaoRepository;
    private final Clock clock;

    public record ConquistasResponse(List<Trilha> trilhas, List<Especial> especiais, List<Disputa> disputas) {
    }

    @Transactional(readOnly = true)
    public ConquistasResponse daPessoa(Long pessoaId) {
        List<Presenca> todas = presencas();
        LocalDate hoje = LocalDate.now(clock);
        return new ConquistasResponse(
                Conquistas.trilhas(todas.stream().filter(p -> pessoaId.equals(p.pessoaId())).toList()),
                Conquistas.especiais(pessoaId, todas, hoje),
                Conquistas.disputas(pessoaId, todas, hoje));
    }

    @Transactional(readOnly = true)
    public List<Destaque> destaquesDoSemestre(int limite) {
        return Conquistas.destaques(presencas(), LocalDate.now(clock), limite);
    }

    // shortcut: lê todas as presenças concluídas a cada consulta; com milhares de eventos, agregar no banco.
    private List<Presenca> presencas() {
        return participacaoRepository.findPresentesPorStatusDoEvento(StatusEventoEnum.FINALIZADO).stream()
                .map(p -> new Presenca(
                        p.getPessoa().getId(),
                        p.getPessoa().getNome(),
                        p.getEvento().getData(),
                        minutos(p.getEvento()),
                        p.getEvento().getDisciplina() != null ? p.getEvento().getDisciplina().getNome() : null,
                        p.getTipo()))
                .toList();
    }

    /** Duração do evento, a mesma base da carga horária do certificado; horário inválido conta zero. */
    static int minutos(Evento evento) {
        try {
            long minutos = Duration.between(LocalTime.parse(evento.getHorarioInicio()),
                    LocalTime.parse(evento.getHorarioFim())).toMinutes();
            return (int) Math.max(0, minutos);
        } catch (DateTimeParseException | NullPointerException e) {
            return 0;
        }
    }
}
