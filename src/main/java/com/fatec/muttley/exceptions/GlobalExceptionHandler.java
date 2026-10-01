package com.fatec.muttley.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MENSAGEM_NAO_AUTENTICADO = "Autenticação obrigatória ou inválida.";
    private static final String MENSAGEM_ACESSO_NEGADO = "Você não tem permissão para acessar este recurso.";
    private final ObjectMapper json = new ObjectMapper();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + Objects.toString(erro.getDefaultMessage(), "valor inválido"))
                .distinct()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "VALIDACAO", "Dados inválidos.", campos, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RespostaErro> requisicaoInvalida(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Requisição inválida ou incompleta.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespostaErro> tipoNaoSuportado(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MIDIA_NAO_SUPORTADA",
                "Tipo de conteúdo não suportado.", request);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<RespostaErro> argumentoInvalido(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", ex.getMessage(), request);
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
    public ResponseEntity<RespostaErro> conflito(Exception ex, HttpServletRequest request) {
        String mensagem = ex instanceof DataIntegrityViolationException
                ? "Operação viola a integridade dos dados ou criaria um registro duplicado." : ex.getMessage();
        return resposta(HttpStatus.CONFLICT, "CONFLITO", mensagem, request);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<RespostaErro> naoEncontrado(Exception ex, HttpServletRequest request) {
        String mensagem = ex instanceof NoResourceFoundException ? "Recurso não encontrado." : ex.getMessage();
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespostaErro> metodoNaoPermitido(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO", "Método HTTP não permitido.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespostaErro> statusExplicito(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            LOG.error("Status HTTP inesperado em {}", request.getRequestURI(), ex);
            return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Erro interno do servidor.", request);
        }
        String mensagem = ex.getReason() == null || ex.getReason().isBlank() ? mensagemPadrao(status) : ex.getReason();
        return resposta(status, codigo(status), mensagem, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErro> naoAutenticado(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", MENSAGEM_NAO_AUTENTICADO, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErro> acessoNegado(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", MENSAGEM_ACESSO_NEGADO, request);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<RespostaErro> servicoExterno(Exception ex, HttpServletRequest request) {
        LOG.warn("Serviço externo indisponível em {}: {}", request.getRequestURI(), ex.toString());
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "SERVICO_INDISPONIVEL",
                "Serviço externo indisponível. Tente novamente em alguns instantes.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> inesperado(Exception ex, HttpServletRequest request) {
        LOG.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Erro interno do servidor.", request);
    }

    /** Usa o mesmo contrato antes do MVC, quando o Spring Security rejeita a requisição. */
    public void escreverNaoAutenticado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", MENSAGEM_NAO_AUTENTICADO);
    }

    public void escreverAcessoNegado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "ACESSO_NEGADO", MENSAGEM_ACESSO_NEGADO);
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String codigo, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getWriter(), corpo(status, codigo, mensagem, List.of(), request));
    }

    private ResponseEntity<RespostaErro> resposta(HttpStatus status, String codigo, String mensagem,
            HttpServletRequest request) {
        return resposta(status, codigo, mensagem, List.of(), request);
    }

    private ResponseEntity<RespostaErro> resposta(HttpStatus status, String codigo, String mensagem,
            List<String> erros, HttpServletRequest request) {
        return ResponseEntity.status(status).body(corpo(status, codigo, mensagem, erros, request));
    }

    private RespostaErro corpo(HttpStatus status, String codigo, String mensagem, List<String> erros,
            HttpServletRequest request) {
        Object rota = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String caminho = rota instanceof String padrao ? padrao : request.getRequestURI();
        return new RespostaErro(status.value(), codigo, mensagem, erros, caminho);
    }

    private String codigo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "REQUISICAO_INVALIDA";
            case UNAUTHORIZED -> "NAO_AUTENTICADO";
            case FORBIDDEN -> "ACESSO_NEGADO";
            case NOT_FOUND -> "NAO_ENCONTRADO";
            case CONFLICT -> "CONFLITO";
            case SERVICE_UNAVAILABLE -> "SERVICO_INDISPONIVEL";
            default -> status.is5xxServerError() ? "ERRO_INTERNO" : "ERRO_HTTP";
        };
    }

    private String mensagemPadrao(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> MENSAGEM_NAO_AUTENTICADO;
            case FORBIDDEN -> MENSAGEM_ACESSO_NEGADO;
            case NOT_FOUND -> "Recurso não encontrado.";
            default -> "Não foi possível concluir a requisição.";
        };
    }
}
