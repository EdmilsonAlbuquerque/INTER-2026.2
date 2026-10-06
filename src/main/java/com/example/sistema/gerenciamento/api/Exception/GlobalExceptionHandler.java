package com.example.sistema.gerenciamento.api.Exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza o tratamento de excecoes para que todos os endpoints retornem
 * o mesmo formato de resposta JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardError> tratarTamanhoFoto(Exception exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter até 5 MB.", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> tratarArgumento(IllegalArgumentException exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> tratarDuplicidade(Exception exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.CONFLICT, "Dados duplicados ou vinculados a outros registros.", request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<StandardError> tratarRota(Exception exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.NOT_FOUND, "Página não encontrada.", request.getRequestURI());
    }

    /** Trata recursos que nao foram encontrados e responde com HTTP 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> tratarNaoEncontrado(
            ResourceNotFoundException exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    /** Trata violacoes de regras de negocio e responde com HTTP 400. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardError> tratarRegraDeNegocio(
            BusinessException exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    /** Trata falhas de autenticacao e responde com HTTP 401. */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<StandardError> tratarNaoAutorizado(
            UnauthorizedException exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    /** Trata erros de permissao e responde com HTTP 403. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> tratarProibido(
            ForbiddenException exception, HttpServletRequest request) {
        return criarResposta(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
    }

    /**
     * Converte cada erro de Bean Validation em um par campo/mensagem para
     * facilitar a exibicao dos problemas no cliente da API.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> tratarValidacao(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> erros = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        erro -> erro.getField(),
                        erro -> erro.getDefaultMessage() == null ? "Valor invalido" : erro.getDefaultMessage(),
                        (mensagemAtual, mensagemNova) -> mensagemAtual,
                        LinkedHashMap::new));

        StandardError resposta = criarErro(
                HttpStatus.BAD_REQUEST, "Erro de validacao dos dados", request.getRequestURI());
        resposta.setErrors(erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }

    /** Evita expor detalhes internos de erros inesperados ao consumidor. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> tratarErroInesperado(
            Exception exception, HttpServletRequest request) {
        return criarResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado.",
                request.getRequestURI());
    }

    /** Cria uma resposta HTTP com o DTO padronizado. */
    private ResponseEntity<StandardError> criarResposta(
            HttpStatus status, String mensagem, String caminho) {
        return ResponseEntity.status(status).body(criarErro(status, mensagem, caminho));
    }

    /** Preenche os dados comuns de qualquer erro da API. */
    private StandardError criarErro(HttpStatus status, String mensagem, String caminho) {
        return StandardError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensagem)
                .path(caminho)
                .build();
    }
}
