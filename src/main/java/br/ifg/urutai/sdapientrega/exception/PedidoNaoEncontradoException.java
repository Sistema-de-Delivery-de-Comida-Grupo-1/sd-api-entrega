package br.ifg.urutai.sdapientrega.exception;

/**
 * Exceção lançada quando um pedido não é encontrado no banco de dados.
 */
public class PedidoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PedidoNaoEncontradoException(String message) {
        super(message);
    }

    public PedidoNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}
