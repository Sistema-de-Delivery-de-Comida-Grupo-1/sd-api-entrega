package br.ifg.urutai.sdapientrega.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para o microserviço de Entrega.
 * 
 * Define:
 * - Exchanges
 * - Filas
 * - Bindings
 * - Configurações de conexão
 */
@Configuration
public class RabbitMQConfig {

    // Exchange
    @Value("${rabbitmq.exchange.entrega}")
    private String exchangeEntrega;

    // Filas
    @Value("${rabbitmq.queue.notificacao}")
    private String queueNotificacao;

    @Value("${rabbitmq.queue.status}")
    private String queueStatus;

    // Routing Keys
    @Value("${rabbitmq.routing-key.notificacao}")
    private String routingKeyNotificacao;

    @Value("${rabbitmq.routing-key.status}")
    private String routingKeyStatus;

    /**
     * Define o exchange do tipo Topic para o domínio de entrega.
     * Topic exchange permite roteamento flexível baseado em padrões.
     */
    @Bean
    public TopicExchange exchangeEntrega() {
        return new TopicExchange(exchangeEntrega, true, false);
    }

    /**
     * Fila para notificações de entrega.
     * Esta fila recebe mensagens quando o status do pedido muda.
     */
    @Bean
    public Queue queueNotificacao() {
        return QueueBuilder.durable(queueNotificacao)
                .build();
    }

    /**
     * Fila para atualizações de status.
     * Esta fila recebe atualizações gerais de status do pedido.
     */
    @Bean
    public Queue queueStatus() {
        return QueueBuilder.durable(queueStatus)
                .build();
    }

    /**
     * Binding entre a fila de notificação e o exchange.
     * Rota mensagens com a chave "entrega.notificacao.*"
     */
    @Bean
    public Binding bindingNotificacao(Queue queueNotificacao, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueNotificacao)
                .to(exchangeEntrega)
                .with(routingKeyNotificacao);
    }

    /**
     * Binding entre a fila de status e o exchange.
     * Rota mensagens com a chave "entrega.status.*"
     */
    @Bean
    public Binding bindingStatus(Queue queueStatus, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueStatus)
                .to(exchangeEntrega)
                .with(routingKeyStatus);
    }
}
