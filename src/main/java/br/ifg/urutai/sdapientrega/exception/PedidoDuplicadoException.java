package br.ifg.urutai.sdapientrega.exception;

/**
 * Exceção lançada quando o pedido já existe no banco de dados.
 */
public class PedidoDuplicadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PedidoDuplicadoException(String message) {
        super(message);
    }

    public PedidoDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
