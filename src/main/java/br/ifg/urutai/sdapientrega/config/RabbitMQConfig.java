package br.ifg.urutai.sdapientrega.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {

    // Exchanges
    @Value("${rabbitmq.exchange.entrega}")
    private String exchangeEntrega;

    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangePedidos;

    // Filas de saída (notificações do sd-api-entrega)
    @Value("${rabbitmq.queue.notificacao}")
    private String queueNotificacao;

    @Value("${rabbitmq.queue.status}")
    private String queueStatus;

    // Fila de entrada (recebe eventos de pedidos do sd-api-pedido via event-notificacao)
    @Value("${rabbitmq.queue.pedido-novo}")
    private String queuePedidoNovo;

    // Routing Keys de saída (binding patterns)
    @Value("${rabbitmq.routing-key.notificacao}")
    private String routingKeyNotificacao;

    @Value("${rabbitmq.routing-key.status}")
    private String routingKeyStatus;

    // Routing Key de entrada
    @Value("${rabbitmq.routing-key.pedido-novo}")
    private String routingKeyPedidoNovo;

    /**
     * Converte mensagens para/de JSON usando Jackson.
     * TypePrecedence.INFERRED ignora o header __TypeId__ do Spring Cloud Stream
     * e usa o tipo do parâmetro do @RabbitListener para deserialização.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(DefaultJackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    /**
     * Configura o RabbitTemplate com o conversor JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Exchange próprio do sd-api-entrega (Topic) para publicar notificações internas.
     */
    @Bean
    public TopicExchange exchangeEntrega() {
        return new TopicExchange(exchangeEntrega, true, false);
    }

    /**
     * Exchange compartilhado event-notificacao (Topic).
     * Publicado pelo sd-api-pedido (Spring Cloud Stream) e consumido por
     * sd-api-entrega e sd-api-notificacao.
     * sd-api-entrega também publica aqui ao mudar status da entrega.
     */
    @Bean
    public TopicExchange exchangePedidos() {
        return new TopicExchange(exchangePedidos, true, false);
    }

    /**
     * Fila para notificações internas de entrega.
     * Recebe eventos de SAIU_PARA_ENTREGA e ENTREGUE via entrega.topic.exchange.
     */
    @Bean
    public Queue queueNotificacao() {
        return QueueBuilder.durable(queueNotificacao).build();
    }

    /**
     * Fila para atualizações de status internas.
     * Recebe demais mudanças de status via entrega.topic.exchange.
     */
    @Bean
    public Queue queueStatus() {
        return QueueBuilder.durable(queueStatus).build();
    }

    /**
     * Fila de entrada de pedidos do sd-api-pedido.
     * Bound ao exchange event-notificacao com routing key "#" para receber
     * todos os eventos publicados pelo sd-api-pedido via Spring Cloud Stream.
     */
    @Bean
    public Queue queuePedidoNovo() {
        return QueueBuilder.durable(queuePedidoNovo).build();
    }

    /**
     * Binding: entrega.notificacao.queue → entrega.topic.exchange (entrega.notificacao.*)
     */
    @Bean
    public Binding bindingNotificacao(Queue queueNotificacao, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueNotificacao)
                .to(exchangeEntrega)
                .with(routingKeyNotificacao);
    }

    /**
     * Binding: entrega.status.queue → entrega.topic.exchange (entrega.status.*)
     */
    @Bean
    public Binding bindingStatus(Queue queueStatus, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueStatus)
                .to(exchangeEntrega)
                .with(routingKeyStatus);
    }

    /**
     * Binding: entrega.pedido.novo.queue → event-notificacao (#)
     * Recebe todos os eventos do sd-api-pedido via Spring Cloud Stream.
     */
    @Bean
    public Binding bindingPedidoNovo(Queue queuePedidoNovo, TopicExchange exchangePedidos) {
        return BindingBuilder.bind(queuePedidoNovo)
                .to(exchangePedidos)
                .with(routingKeyPedidoNovo);
    }
}
