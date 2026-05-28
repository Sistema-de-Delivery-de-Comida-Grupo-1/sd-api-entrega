package br.ifg.urutai.sdapientrega.exception;

/**
 * Exceção lançada quando ocorre uma falha ao enviar mensagem para o RabbitMQ.
 */
public class ErroAoEnviarNotificacaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErroAoEnviarNotificacaoException(String message) {
        super(message);
    }

    public ErroAoEnviarNotificacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
