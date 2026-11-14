package com.fatec.muttley.pessoa;

public record PessoaMeResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String cpf,
        Role role,
        boolean temFoto
) {
    public static PessoaMeResponse from(Pessoa pessoa, boolean temFoto) {
        return new PessoaMeResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getTelefone(),
                pessoa.getCpf(),
                pessoa.getRole(),
                temFoto
        );
    }
}
