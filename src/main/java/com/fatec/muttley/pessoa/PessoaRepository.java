package com.fatec.muttley.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
@Transactional
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByCpf(String cpf);

    Optional<Pessoa> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<Pessoa> findByCadastroTokenHash(String cadastroTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pessoa> findWithLockByCadastroTokenHash(String cadastroTokenHash);
}
