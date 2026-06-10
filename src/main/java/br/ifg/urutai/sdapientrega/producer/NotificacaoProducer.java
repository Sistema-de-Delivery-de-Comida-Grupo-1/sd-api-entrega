package br.ifg.urutai.sdapientrega.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.ifg.urutai.sdapientrega.dto.NotificacaoDTO;
import br.ifg.urutai.sdapientrega.dto.PedidoEventDTO;

@Component
public class NotificacaoProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Exchange próprio do sd-api-entrega (entrega.topic.exchange) */
    @Value("${rabbitmq.exchange.entrega}")
    private String exchangeEntrega;

    /** Exchange compartilhado com sd-api-pedido e sd-api-notificacao (event-notificacao) */
    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangeEventNotificacao;

    // Routing keys específicas para entrega.topic.exchange
    @Value("${rabbitmq.routing-key.notificacao.saida}")
    private String routingKeySaida;

    @Value("${rabbitmq.routing-key.notificacao.entregue}")
    private String routingKeyEntregue;

    @Value("${rabbitmq.routing-key.status.atualizado}")
    private String routingKeyStatusAtualizado;

    private final Logger log = LoggerFactory.getLogger(NotificacaoProducer.class);

    // -------------------------------------------------------------------------
    // Publicação para sd-api-notificacao via event-notificacao exchange
    // -------------------------------------------------------------------------

    /**
     * Publica um evento de status de entrega no exchange {@code event-notificacao}
     * para consumo pelo sd-api-notificacao ({@code PedidoEventListener}).
     *
     * <p>O sd-api-notificacao tem a fila {@code queue.pedido-entrega} bound a este
     * exchange com routing key {@code #}, portanto receberá a mensagem independente
     * da routing key usada.
     *
     * @param pedidoId  ID do pedido original (sd-api-pedido)
     * @param idCliente ID do cliente
     * @param status    status do evento, ex: {@code "SAIU_PARA_ENTREGA"}, {@code "ENTREGUE"}
     * @param valorTotal valor total do pedido
     */
    public void enviarEventoParaNotificacao(Long pedidoId, Long idCliente, String status, double valorTotal) {
        try {
            PedidoEventDTO event = PedidoEventDTO.builder()
                    .id(pedidoId)
                    .idCliente(idCliente)
                    .status(status)
                    .valorTotal(valorTotal)
                    .build();

            // Routing key "entrega.evento" – sd-api-notificacao usa binding "#" então qualquer key funciona
            rabbitTemplate.convertAndSend(exchangeEventNotificacao, "entrega.evento", event);

            log.info("Evento publicado para sd-api-notificacao via event-notificacao. "
                    + "pedidoId: {} – status: {}", pedidoId, status);
        } catch (Exception e) {
            log.error("Erro ao publicar evento para sd-api-notificacao. pedidoId: {} – Erro: {}",
                    pedidoId, e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Publicação interna via entrega.topic.exchange
    // -------------------------------------------------------------------------

    /**
     * Envia notificação quando o pedido sai para entrega.
     * Routing key: {@code entrega.notificacao.saida}
     */
    public void enviarNotificacaoSaidaEntrega(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando notificação de saída para entrega. Pedido: {} - Cliente: {}",
                    notificacao.getIdPedido(), notificacao.getIdCliente());

            notificacao.setTimestamp(System.currentTimeMillis());
            notificacao.setTipoEvento("SAIU_PARA_ENTREGA");

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeySaida, notificacao);

            log.info("Notificação de saída enviada. Pedido: {} – routing-key: {}",
                    notificacao.getIdPedido(), routingKeySaida);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de saída. Pedido: {} – Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar notificação de saída para entrega", e);
        }
    }

    /**
     * Envia notificação quando o pedido é entregue.
     * Routing key: {@code entrega.notificacao.entregue}
     */
    public void enviarNotificacaoEntregue(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando notificação de entrega concluída. Pedido: {} - Cliente: {}",
                    notificacao.getIdPedido(), notificacao.getIdCliente());

            notificacao.setTimestamp(System.currentTimeMillis());
            notificacao.setTipoEvento("ENTREGUE");

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeyEntregue, notificacao);

            log.info("Notificação de entrega concluída enviada. Pedido: {} – routing-key: {}",
                    notificacao.getIdPedido(), routingKeyEntregue);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de entrega. Pedido: {} – Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar notificação de entrega concluída", e);
        }
    }

    /**
     * Envia atualização genérica de status.
     * Routing key: {@code entrega.status.atualizado}
     */
    public void enviarAtualizacaoStatus(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando atualização de status. Pedido: {} – Status: {}",
                    notificacao.getIdPedido(), notificacao.getTipoEvento());

            notificacao.setTimestamp(System.currentTimeMillis());

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeyStatusAtualizado, notificacao);

            log.info("Atualização de status enviada. Pedido: {} – routing-key: {}",
                    notificacao.getIdPedido(), routingKeyStatusAtualizado);
        } catch (Exception e) {
            log.error("Erro ao enviar atualização de status. Pedido: {} – Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar atualização de status", e);
        }
    }
}
