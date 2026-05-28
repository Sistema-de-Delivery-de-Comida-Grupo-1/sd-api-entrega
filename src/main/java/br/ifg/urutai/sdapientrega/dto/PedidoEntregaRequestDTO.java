package br.ifg.urutai.sdapientrega.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para requisição de criação/atualização de Pedido de Entrega.
 * Utilizado para validar dados de entrada nas operações REST.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntregaRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Nome do cliente não pode estar vazio")
    @Schema(description = "Nome completo do cliente", example = "João Silva", required = true)
    private String nomeCliente;

    @NotBlank(message = "Endereço não pode estar vazio")
    @Schema(description = "Endereço completo de entrega", 
            example = "Rua Principal, 123, Apto 456, São Paulo - SP, 01310-100", 
            required = true)
    private String endereco;

    @NotBlank(message = "Número do pedido não pode estar vazio")
    @Schema(description = "Número único do pedido", example = "PED-2024-001", required = true)
    private String numeroPedido;

    @NotBlank(message = "Método de pagamento não pode estar vazio")
    @Schema(description = "Método de pagamento utilizado", example = "CARTÃO_CRÉDITO", required = true)
    private String metodoPagamento;
}
