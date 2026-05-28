package br.ifg.urutai.sdapientrega.dto;

import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para atualização de status de um Pedido de Entrega.
 * Utilizado para validar dados de entrada na operação de atualização de status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Status não pode ser nulo")
    @Schema(description = "Novo status do pedido", example = "SAIU_PARA_ENTREGA", required = true)
    private PedidoStatus status;
}
