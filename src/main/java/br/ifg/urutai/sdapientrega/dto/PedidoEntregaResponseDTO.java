package br.ifg.urutai.sdapientrega.dto;

import br.ifg.urutai.sdapientrega.enums.PedidoStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para resposta de Pedido de Entrega.
 * Utilizado para serializar dados do pedido em respostas REST.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEntregaResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID único do pedido", example = "1")
    private Long id;

    @Schema(description = "Nome completo do cliente", example = "João Silva")
    private String nomeCliente;

    @Schema(description = "Endereço completo de entrega", 
            example = "Rua Principal, 123, Apto 456, São Paulo - SP, 01310-100")
    private String endereco;

    @Schema(description = "Número único do pedido", example = "PED-2024-001")
    private String numeroPedido;

    @Schema(description = "Método de pagamento utilizado", example = "CARTÃO_CRÉDITO")
    private String metodoPagamento;

    @Schema(description = "Status atual do pedido", example = "RECEBIDO")
    private PedidoStatus status;

    @Schema(description = "Data e hora de criação do pedido")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do pedido")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataAtualizacao;
}
