package com.fatec.muttley.painel;

import com.fatec.muttley.certificado.CertificadoRepository;
import com.fatec.muttley.evento.EventoRepository;
import com.fatec.muttley.pessoa.PessoaRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Busca as contagens no banco e entrega o painel da coordenação já calculado. */
@Service
@RequiredArgsConstructor
public class PainelAdminService {
    private final Clock clock;
    private final EventoRepository eventoRepository;
    private final CertificadoRepository certificadoRepository;
    private final PessoaRepository pessoaRepository;

    @Transactional(readOnly = true)
    public PainelAdmin.Resultado montar() {
        LocalDate hoje = LocalDate.now(clock);
        return PainelAdmin.montar(
                eventoRepository.findEstatisticasDosEventos(),
                certificadoRepository.findDatasDeEmissaoDesde(PainelAdmin.inicioDaBusca(hoje)),
                hoje,
                pessoaRepository.countBySenhaIsNull());
    }
}
