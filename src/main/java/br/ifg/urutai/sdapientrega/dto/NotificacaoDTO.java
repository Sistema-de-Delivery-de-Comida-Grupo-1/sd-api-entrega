package br.ifg.urutai.sdapientrega.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para envio de notificações via RabbitMQ.
 * Utilizado para comunicação entre o Delivery Service e o serviço de Notificação.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Número do pedido", example = "PED-2024-001")
    private String numeroPedido;

    @Schema(description = "Nome do cliente", example = "João Silva")
    private String nomeCliente;

    @Schema(description = "Mensagem da notificação", example = "Seu pedido saiu para entrega.")
    private String mensagem;

    @Schema(description = "Tipo de evento", example = "SAIU_PARA_ENTREGA")
    private String tipoEvento;

    @Schema(description = "Timestamp de envio da notificação")
    private Long timestamp;
}
