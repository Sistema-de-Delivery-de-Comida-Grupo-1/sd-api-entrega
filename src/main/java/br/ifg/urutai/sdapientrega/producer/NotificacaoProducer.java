package br.ifg.urutai.sdapientrega.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.ifg.urutai.sdapientrega.consumer.PedidoEntregaConsumer;
import br.ifg.urutai.sdapientrega.dto.NotificacaoDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * Componente responsável por enviar mensagens de notificação para o RabbitMQ.
 *
 * Este producer envia eventos de mudança de status do pedido para serem
 * consumidos pelo serviço de Notificação.
 */
@Slf4j
@Component
public class NotificacaoProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.entrega}")
    private String exchangeEntrega;

    @Value("${rabbitmq.routing-key.notificacao}")
    private String routingKeyNotificacao;

    @Value("${rabbitmq.routing-key.status}")
    private String routingKeyStatus;

    private Logger log = LoggerFactory.getLogger(PedidoEntregaConsumer.class);

    /**
     * Envia notificação quando o pedido sai para entrega.
     *
     * @param notificacao DTO com dados da notificação
     */
    public void enviarNotificacaoSaidaEntrega(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando notificação de saída para entrega. Pedido: {} - Cliente: {}",
                    notificacao.getIdPedido(), notificacao.getIdCliente());

            notificacao.setTimestamp(System.currentTimeMillis());
            notificacao.setTipoEvento("SAIU_PARA_ENTREGA");

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeyNotificacao, notificacao);

            log.info("Notificação enviada com sucesso. Pedido: {}", notificacao.getIdPedido());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de saída para entrega. Pedido: {} - Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar notificação de saída para entrega", e);
        }
    }

    /**
     * Envia notificação quando o pedido é entregue.
     *
     * @param notificacao DTO com dados da notificação
     */
    public void enviarNotificacaoEntregue(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando notificação de entrega. Pedido: {} - Cliente: {}",
                    notificacao.getIdPedido(), notificacao.getIdCliente());

            notificacao.setTimestamp(System.currentTimeMillis());
            notificacao.setTipoEvento("ENTREGUE");

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeyNotificacao, notificacao);

            log.info("Notificação de entrega enviada com sucesso. Pedido: {}", notificacao.getIdPedido());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de entrega. Pedido: {} - Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar notificação de entrega", e);
        }
    }

    /**
     * Envia atualização de status para o tópico de status.
     *
     * @param notificacao DTO com dados da notificação de status
     */
    public void enviarAtualizacaoStatus(NotificacaoDTO notificacao) {
        try {
            log.info("Enviando atualização de status. Pedido: {} - Status: {}",
                    notificacao.getIdPedido(), notificacao.getTipoEvento());

            notificacao.setTimestamp(System.currentTimeMillis());

            rabbitTemplate.convertAndSend(exchangeEntrega, routingKeyStatus, notificacao);

            log.info("Atualização de status enviada com sucesso. Pedido: {}", notificacao.getIdPedido());
        } catch (Exception e) {
            log.error("Erro ao enviar atualização de status. Pedido: {} - Erro: {}",
                    notificacao.getIdPedido(), e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar atualização de status", e);
        }
    }
}
