package org.example.message;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageMqConfig {

    @Value("${app.mq.exchange:chat.exchange}")
    private String exchangeName;

    @Value("${app.mq.queue:chat.bot}")
    private String queueName;

    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue chatQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(routingKey);
    }
}
