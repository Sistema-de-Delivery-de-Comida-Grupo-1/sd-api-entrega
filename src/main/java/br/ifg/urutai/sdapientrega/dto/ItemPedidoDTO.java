package br.ifg.urutai.sdapientrega.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa um item de pedido recebido do serviço de Pedidos
 * (sd-api-pedido).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID do item do pedido", example = "1")
    private Long id;

    @Schema(description = "ID do produto", example = "42")
    private Long idProduto;

    @Schema(description = "Quantidade do produto no pedido", example = "2")
    private int quantidade;

    @Schema(description = "Valor unitário do produto em centavos", example = "1990")
    private int valorUnitario;
}
