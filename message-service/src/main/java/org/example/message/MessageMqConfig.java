package org.example.message;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * MQ-topologi för Message-tjänsten (RabbitMQ).
 *
 * Översikt:
 * - Skapar ett TopicExchange, en Queue och en Binding emellan dem.
 * - Alla namn och routing‑nycklar är konfigurerbara via properties med rimliga defaults.
 *
 * Syfte:
 * - Möjliggöra enkel publicering/lyssning på händelsen "message.published".
 */
public class MessageMqConfig {

    @Value("${app.mq.exchange:chat.exchange}")
    private String exchangeName;

    @Value("${app.mq.queue:chat.bot}")
    private String queueName;

    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    @Bean
    /**
     * Skapar ett hållbart TopicExchange.
     * @return TopicExchange med angivet namn
     */
    public TopicExchange chatExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    /**
     * Skapar en hållbar kö (ej auto-delete).
     * @return Queue med angivet namn
     */
    public Queue chatQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    /**
     * Binder kön till exchanget med routing‑nyckeln.
     */
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(routingKey);
    }
}
