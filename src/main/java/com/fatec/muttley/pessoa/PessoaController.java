package com.fatec.muttley.pessoa;

import com.fatec.muttley.aluno.AlunoService;
import com.fatec.muttley.auth.dto.RegisterInfo;
import com.fatec.muttley.colaborador.ColaboradorService;
import com.fatec.muttley.organizador.OrganizadorService;
import com.fatec.muttley.palestrante.PalestranteService;
import com.fatec.muttley.professor.ProfessorService;
import com.fatec.muttley.security.HashIdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Pessoas e Perfis", description = "Gestão administrativa de pessoas, perfis de atuação e consulta pública de dados para cadastro")
@RestController
@RequestMapping("/api")
public class PessoaController {

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private PessoaMapper pessoaMapper;

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private ProfessorService professorService;

    @Autowired
    private PalestranteService palestranteService;

    @Autowired
    private OrganizadorService organizadorService;

    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private HashIdService hashIdService;

    @Operation(summary = "Listar alunos (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/alunos")
    public ResponseEntity<List<?>> listarAlunos() {
        return ResponseEntity.ok(alunoService.procurarTodos());
    }

    @Operation(summary = "Listar professores (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/professores")
    public ResponseEntity<List<?>> listarProfessores() {
        return ResponseEntity.ok(professorService.procurarTodos());
    }

    @Operation(summary = "Listar palestrantes (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/palestrantes")
    public ResponseEntity<List<?>> listarPalestrantes() {
        return ResponseEntity.ok(palestranteService.procurarTodos());
    }

    @Operation(summary = "Listar organizadores (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/organizadores")
    public ResponseEntity<List<?>> listarOrganizadores() {
        return ResponseEntity.ok(organizadorService.procurarTodos());
    }

    @Operation(summary = "Listar colaboradores (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/colaboradores")
    public ResponseEntity<List<?>> listarColaboradores() {
        return ResponseEntity.ok(colaboradorService.procurarTodos());
    }

    @Operation(summary = "Listar todas as pessoas (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/pessoas")
    public ResponseEntity<List<Pessoa>> listarTodos() {
        return ResponseEntity.ok(pessoaService.procurarTodos());
    }

    @Operation(summary = "Buscar pessoa por ID (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/pessoas/{id}")
    public ResponseEntity<AtualizacaoPessoa> buscarPorId(
            @Parameter(description = "ID da pessoa") @PathVariable Long id) {
        Pessoa pessoa = pessoaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
        return ResponseEntity.ok(pessoaMapper.toAtualizacaoDto(pessoa));
    }

    @Operation(summary = "Cadastrar pessoa (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin/pessoas")
    public ResponseEntity<Map<String, String>> criar(@RequestBody @Valid AtualizacaoPessoa dto) {
        Pessoa pessoaSalva = pessoaService.salvarOuAtualizar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Pessoa '" + pessoaSalva.getNome() + "' criada com sucesso!"));
    }

    @Operation(summary = "Atualizar pessoa existente (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/admin/pessoas/{id}")
    public ResponseEntity<Map<String, String>> atualizar(
            @Parameter(description = "ID da pessoa") @PathVariable Long id,
            @RequestBody @Valid AtualizacaoPessoa dto) {
        pessoaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
        dto = dto.withId(id);
        Pessoa pessoaSalva = pessoaService.salvarOuAtualizar(dto);
        return ResponseEntity.ok(Map.of("message", "Pessoa '" + pessoaSalva.getNome() + "' atualizada com sucesso!"));
    }

    @Operation(summary = "Excluir pessoa (Administração)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/admin/pessoas/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> deletar(
            @Parameter(description = "ID da pessoa") @PathVariable Long id) {
        pessoaService.procurarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
        pessoaService.apagarPorId(id);
        return ResponseEntity.ok(Map.of("message", "Pessoa " + id + " foi apagada!"));
    }

    @Operation(summary = "Consultar dados para conclusão de cadastro via hashId",
            description = "Endpoint público utilizado pelo participante via link de email para preenchimento de cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados cadastrais retornados"),
            @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    })
    @GetMapping("/pessoa/dados-cadastro/{id}")
    public RegisterInfo getInfo(
            @Parameter(description = "Código hashId codificado da pessoa") @PathVariable String id) {
        Long pessoaId = hashIdService.decode(id);
        Pessoa pessoa = pessoaService.procurarPorId(pessoaId)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        return new RegisterInfo(
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getCpf()
        );
    }
}