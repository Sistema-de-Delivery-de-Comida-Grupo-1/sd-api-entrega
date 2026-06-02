package br.ifg.urutai.sdapientrega.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de Pedido de Entrega. Utilizado para serializar dados do
 * pedido em respostas REST.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntregaResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID interno do registro de entrega", example = "1")
    private Long id;

    @Schema(description = "ID do pedido no serviço de origem (sd-api-pedido)", example = "10")
    private Long idPedido;

    @Schema(description = "ID do cliente que realizou o pedido", example = "42")
    private Long idCliente;

    @Schema(description = "Valor total do pedido em centavos", example = "3980")
    private int valorTotal;

    @Schema(description = "Status atual da entrega", example = "SAIU_PARA_ENTREGA")
    private PedidoStatus status;

    @Schema(description = "Data e hora de criação do registro de entrega")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do registro de entrega")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataAtualizacao;
}
