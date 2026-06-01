package br.ifg.urutai.sdapientrega.consumer;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import br.ifg.urutai.sdapientrega.dto.PedidoEntregaRequestDTO;
import br.ifg.urutai.sdapientrega.exception.PedidoDuplicadoException;
import br.ifg.urutai.sdapientrega.service.PedidoEntregaService;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer responsável por receber novos pedidos via RabbitMQ.
 *
 * Escuta a fila {@code entrega.pedido.novo.queue}, publicada pelo serviço de
 * Pedidos (sd-api-pedido) com routing key {@code pedido.criado}.
 *
 * Fluxo:
 * 1. Serviço de Pedidos publica um evento "pedido.criado" no exchange "pedidos.topic.exchange"
 * 2. Este consumer recebe a mensagem e cria o PedidoEntrega correspondente
 * 3. O pedido é persistido com status inicial RECEBIDO
 * 4. Em caso de pedido duplicado, a mensagem é descartada (ack) sem reprocessamento
 * 5. Em caso de erro inesperado, a mensagem é rejeitada (nack) e devolvida à fila
 */
@Slf4j
@Component
public class PedidoEntregaConsumer {

    @Autowired
    private PedidoEntregaService pedidoEntregaService;

    /**
     * Recebe um novo pedido da fila e registra no sistema de entrega.
     *
     * O acknowledge é feito manualmente para garantir que a mensagem só seja
     * removida da fila após processamento bem-sucedido.
     *
     * @param requestDTO dados do pedido recebido via fila
     * @param channel    canal AMQP para controle de acknowledge manual
     * @param deliveryTag tag de entrega da mensagem para ack/nack
     */
    @RabbitListener(queues = "${rabbitmq.queue.pedido-novo}")
    public void receberNovoPedido(
            PedidoEntregaRequestDTO requestDTO,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Pedido recebido via fila. Número: {} - Cliente: {}",
                requestDTO.getNumeroPedido(), requestDTO.getNomeCliente());

        try {
            pedidoEntregaService.criarPedido(requestDTO);

            log.info("Pedido processado com sucesso. Número: {}", requestDTO.getNumeroPedido());
            channel.basicAck(deliveryTag, false);

        } catch (PedidoDuplicadoException e) {
            log.warn("Pedido duplicado ignorado. Número: {} - Mensagem descartada da fila. Detalhe: {}",
                    requestDTO.getNumeroPedido(), e.getMessage());
            // Descarta a mensagem sem reprocessamento (requeue=false)
            rejectMessage(channel, deliveryTag, false);

        } catch (Exception e) {
            log.error("Erro inesperado ao processar pedido. Número: {} - Erro: {}",
                    requestDTO.getNumeroPedido(), e.getMessage(), e);
            // Rejeita e devolve à fila para nova tentativa (requeue=true)
            rejectMessage(channel, deliveryTag, true);
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
