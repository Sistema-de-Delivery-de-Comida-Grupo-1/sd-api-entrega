package br.ifg.urutai.sdapientrega.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recebimento de pedidos publicados pelo serviço de Pedidos
 * (sd-api-pedido) via RabbitMQ ou requisição REST.
 *
 * Mapeia diretamente a entidade {@code Pedido} do serviço de origem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PedidoEntregaRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID do pedido não pode ser nulo")
    @Schema(description = "ID único do pedido no serviço de Pedidos", example = "1", required = true)
    private Long id;

    @NotNull(message = "ID do cliente não pode ser nulo")
    @Schema(description = "ID do cliente que realizou o pedido", example = "42", required = true)
    private Long idCliente;

    @Schema(description = "Valor total do pedido", example = "39.80")
    private double valorTotal;

    @Schema(description = "Itens que compõem o pedido")
    private List<ItemPedidoDTO> itens;

    @Schema(description = "Status do pedido no serviço de origem", example = "PAGO")
    private String status;
}
