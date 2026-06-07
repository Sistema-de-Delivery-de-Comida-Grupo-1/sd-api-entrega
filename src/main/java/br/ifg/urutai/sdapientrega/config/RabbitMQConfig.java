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

/**
 * Configuração do RabbitMQ para o microserviço de Entrega.
 *
 * Define: - Exchanges - Filas - Bindings - Configurações de conexão
 */
@Configuration
public class RabbitMQConfig {

    // Exchanges
    @Value("${rabbitmq.exchange.entrega}")
    private String exchangeEntrega;

    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangePedidos;

    // Filas de saída
    @Value("${rabbitmq.queue.notificacao}")
    private String queueNotificacao;

    @Value("${rabbitmq.queue.status}")
    private String queueStatus;

    // Fila de entrada (recebimento de pedidos)
    @Value("${rabbitmq.queue.pedido-novo}")
    private String queuePedidoNovo;

    // Routing Keys de saída
    @Value("${rabbitmq.routing-key.notificacao}")
    private String routingKeyNotificacao;

    @Value("${rabbitmq.routing-key.status}")
    private String routingKeyStatus;

    // Routing Key de entrada
    @Value("${rabbitmq.routing-key.pedido-novo}")
    private String routingKeyPedidoNovo;

    /**
     * Converte mensagens para/de JSON usando Jackson. Configurado com
     * TypePrecedence.INFERRED para ignorar o header __TypeId__ enviado pelo
     * Spring Cloud Stream (sd-api-pedido) e usar o tipo do parâmetro do
     *
     * @RabbitListener para deserialização.
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
     * Define o exchange do tipo Topic para o domínio de entrega. Topic exchange
     * permite roteamento flexível baseado em padrões.
     */
    @Bean
    public TopicExchange exchangeEntrega() {
        return new TopicExchange(exchangeEntrega, true, false);
    }

    /**
     * Define o exchange do tipo Topic para recebimento de pedidos. Este
     * exchange é publicado pelo serviço de Pedidos (sd-api-pedido).
     */
    @Bean
    public TopicExchange exchangePedidos() {
        return new TopicExchange(exchangePedidos, true, false);
    }

    /**
     * Fila para notificações de entrega. Esta fila recebe mensagens quando o
     * status do pedido muda.
     */
    @Bean
    public Queue queueNotificacao() {
        return QueueBuilder.durable(queueNotificacao)
                .build();
    }

    /**
     * Fila para atualizações de status. Esta fila recebe atualizações gerais de
     * status do pedido.
     */
    @Bean
    public Queue queueStatus() {
        return QueueBuilder.durable(queueStatus)
                .build();
    }

    /**
     * Fila para recebimento de novos pedidos vindos do serviço de Pedidos.
     * Mensagens publicadas com routing key "pedido.criado" chegam aqui.
     */
    @Bean
    public Queue queuePedidoNovo() {
        return QueueBuilder.durable(queuePedidoNovo).build();
    }

    /**
     * Binding entre a fila de notificação e o exchange. Rota mensagens com a
     * chave "entrega.notificacao.*"
     */
    @Bean
    public Binding bindingNotificacao(Queue queueNotificacao, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueNotificacao)
                .to(exchangeEntrega)
                .with(routingKeyNotificacao);
    }

    /**
     * Binding entre a fila de status e o exchange. Rota mensagens com a chave
     * "entrega.status.*"
     */
    @Bean
    public Binding bindingStatus(Queue queueStatus, TopicExchange exchangeEntrega) {
        return BindingBuilder.bind(queueStatus)
                .to(exchangeEntrega)
                .with(routingKeyStatus);
    }

    /**
     * Binding entre a fila de pedidos novos e o exchange "event-notificacao".
     * Usa routing key "#" para receber todas as mensagens publicadas pelo
     * sd-api-pedido via Spring Cloud Stream.
     */
    @Bean
    public Binding bindingPedidoNovo(Queue queuePedidoNovo, TopicExchange exchangePedidos) {
        return BindingBuilder.bind(queuePedidoNovo)
                .to(exchangePedidos)
                .with(routingKeyPedidoNovo);
    }
}
