package br.ifg.urutai.sdapientrega.enums;

/**
 * Enum que representa os possíveis status de um pedido de entrega.
 * 
 * Estados do ciclo de vida de um pedido:
 * - RECEBIDO: Pedido foi recebido no sistema
 * - PREPARANDO_ENTREGA: Pedido está sendo preparado para saída
 * - SAIU_PARA_ENTREGA: Pedido saiu para entrega
 * - ENTREGUE: Pedido foi entregue
 * - CONFIRMADO_PELO_CLIENTE: Cliente confirmou o recebimento
 */
public enum PedidoStatus {
    RECEBIDO("Pedido Recebido"),
    PREPARANDO_ENTREGA("Preparando para Entrega"),
    SAIU_PARA_ENTREGA("Saiu para Entrega"),
    ENTREGUE("Entregue"),
    CONFIRMADO_PELO_CLIENTE("Confirmado pelo Cliente");

    private final String descricao;

    PedidoStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
