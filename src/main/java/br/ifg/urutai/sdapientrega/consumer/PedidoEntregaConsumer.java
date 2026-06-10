package br.ifg.urutai.sdapientrega.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import br.ifg.urutai.sdapientrega.dto.PedidoEntregaRequestDTO;
import br.ifg.urutai.sdapientrega.exception.PedidoDuplicadoException;
import br.ifg.urutai.sdapientrega.service.PedidoEntregaService;
@Component
public class PedidoEntregaConsumer {

    private static final String STATUS_PRONTO_PARA_ENTREGA = "PRONTO_PARA_ENTREGA";

    @Autowired
    private PedidoEntregaService pedidoEntregaService;

    private final Logger log = LoggerFactory.getLogger(PedidoEntregaConsumer.class);

    // -------------------------------------------------------------------------
    // Canal 1: exchange event-notificacao (Spring Cloud Stream)
    // Recebe todos os eventos do sd-api-pedido; filtra por PRONTO_PARA_ENTREGA
    // -------------------------------------------------------------------------

    /**
     * Recebe eventos de mudança de status do sd-api-pedido via exchange
     * {@code event-notificacao} (Spring Cloud Stream).
     *
     * <p>Somente mensagens com {@code status == PRONTO_PARA_ENTREGA} disparam
     * a criação de um registro de entrega. Outros status são confirmados (ack)
     * e descartados sem processamento.
     *
     * @param requestDTO dados do pedido recebido
     * @param channel    canal AMQP para acknowledge manual
     * @param deliveryTag tag de entrega da mensagem
     */
    @RabbitListener(queues = "${rabbitmq.queue.pedido-novo}")
    public void receberEventoPedido(
            PedidoEntregaRequestDTO requestDTO,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("[Canal event-notificacao] Evento recebido. idPedido: {} - status: {}",
                requestDTO.getId(), requestDTO.getStatus());

        // Filtrar: só processar quando o pedido está pronto para entrega
        if (!STATUS_PRONTO_PARA_ENTREGA.equals(requestDTO.getStatus())) {
            log.debug("[Canal event-notificacao] Status '{}' ignorado para idPedido: {}. "
                    + "Somente PRONTO_PARA_ENTREGA cria registro de entrega.",
                    requestDTO.getStatus(), requestDTO.getId());
            ackSilently(channel, deliveryTag);
            return;
        }

        processarPedidoProntoParaEntrega(requestDTO, channel, deliveryTag, "event-notificacao");
    }

    // -------------------------------------------------------------------------
    // Lógica compartilhada de processamento
    // -------------------------------------------------------------------------

    /**
     * Processa um pedido pronto para entrega, criando o registro no sistema.
     *
     * @param requestDTO  dados do pedido
     * @param channel     canal AMQP
     * @param deliveryTag tag da mensagem
     * @param origem      identificador do canal de origem (para logging)
     */
    private void processarPedidoProntoParaEntrega(
            PedidoEntregaRequestDTO requestDTO,
            Channel channel,
            long deliveryTag,
            String origem) {

        try {
            pedidoEntregaService.criarPedido(requestDTO);

            log.info("[{}] Pedido registrado com sucesso para entrega. idPedido: {}",
                    origem, requestDTO.getId());
            ackSilently(channel, deliveryTag);

        } catch (PedidoDuplicadoException e) {
            log.warn("[{}] Pedido duplicado ignorado. idPedido: {} – Detalhe: {}",
                    origem, requestDTO.getId(), e.getMessage());
            // Descarta sem requeue: o pedido já foi registrado pelo outro canal
            rejectMessage(channel, deliveryTag, false);

        } catch (Exception e) {
            log.error("[{}] Erro inesperado ao processar pedido. idPedido: {} – Erro: {}",
                    origem, requestDTO.getId(), e.getMessage(), e);
            // Rejeita e recoloca na fila para nova tentativa
            rejectMessage(channel, deliveryTag, true);
        }
    }

    /**
     * Confirma (ack) uma mensagem de forma silenciosa, capturando IOExceptions.
     */
    private void ackSilently(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("Falha ao confirmar mensagem (deliveryTag={}): {}",
                    deliveryTag, e.getMessage(), e);
        }
    }

    /**
     * Rejeita uma mensagem AMQP de forma segura, capturando eventuais IOExceptions.
     *
     * @param channel     canal AMQP
     * @param deliveryTag tag da mensagem
     * @param requeue     true para devolver à fila, false para descartar
     */
    private void rejectMessage(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException ioException) {
            log.error("Falha ao rejeitar mensagem (deliveryTag={}): {}",
                    deliveryTag, ioException.getMessage(), ioException);
        }
    }
}
