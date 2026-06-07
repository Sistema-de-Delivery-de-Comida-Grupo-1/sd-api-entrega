package br.ifg.urutai.sdapientrega.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemPedidoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID do item do pedido", example = "1")
    private Long id;

    @Schema(description = "Quantidade do produto no pedido", example = "2")
    private int quantidade;

    @Schema(description = "Preço unitário do produto", example = "19.90")
    private double preco;
}
