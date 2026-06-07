package br.ifg.urutai.sdapientrega.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import br.ifg.urutai.sdapientrega.consumer.PedidoEntregaConsumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler global de exceções para o microserviço.
 * 
 * Responsável por:
 * - Tratar exceções de negócio
 * - Tratar exceções de validação
 * - Tratar exceções genéricas
 * - Retornar respostas consistentes de erro
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        private Logger log = LoggerFactory.getLogger(PedidoEntregaConsumer.class);
    /**
     * Trata a exceção quando um pedido não é encontrado.
     */
    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handlePedidoNaoEncontrado(
            PedidoNaoEncontradoException e, WebRequest request) {
        
        log.error("Erro: Pedido não encontrado - {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Pedido Não Encontrado")
                .message(e.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Trata a exceção quando um pedido duplicado é enviado.
     */
    @ExceptionHandler(PedidoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handlePedidoDuplicado(
            PedidoDuplicadoException e, WebRequest request) {
        
        log.error("Erro: Pedido duplicado - {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Pedido Duplicado")
                .message(e.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Trata a exceção quando há erro ao enviar notificação.
     */
    @ExceptionHandler(ErroAoEnviarNotificacaoException.class)
    public ResponseEntity<ErrorResponse> handleErroAoEnviarNotificacao(
            ErroAoEnviarNotificacaoException e, WebRequest request) {
        
        log.error("Erro ao enviar notificação - {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Erro ao Enviar Notificação")
                .message(e.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Trata exceções de validação de Bean Validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException e, WebRequest request) {
        
        log.error("Erro de validação nos dados da requisição");
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Erro de Validação")
                .message("Um ou mais campos contêm dados inválidos")
                .validationErrors(errors)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata todas as outras exceções não tratadas especificamente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception e, WebRequest request) {
        
        log.error("Erro não esperado", e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Erro Interno do Servidor")
                .message("Ocorreu um erro inesperado no processamento da requisição")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
