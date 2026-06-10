package br.ifg.urutai.sdapientrega.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID do pedido", example = "1")
    private Long id;

    @Schema(description = "ID do cliente", example = "42")
    private Long idCliente;

    @Schema(description = "Status do evento de entrega", example = "SAIU_PARA_ENTREGA")
    private String status;

    @Schema(description = "Valor total do pedido", example = "39.80")
    private double valorTotal;
}
