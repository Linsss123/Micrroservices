// Paketdeklaration för MQ-konfiguration i message-tjänsten
package org.example.message;

// AMQP-typer: Binding, Queue och TopicExchange
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
// @Value för att läsa in konfigurerade MQ-namn
import org.springframework.beans.factory.annotation.Value;
// Spring-konfiguration och bean-definitioner
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Markerar denna klass som en konfigurationsklass
@Configuration
public class MessageMqConfig {

    // Konfigurerbart namn på exchange (default: chat.exchange)
    @Value("${app.mq.exchange:chat.exchange}")
    private String exchangeName;

    // Konfigurerbart namn på kön (default: chat.bot)
    @Value("${app.mq.queue:chat.bot}")
    private String queueName;

    // Konfigurerbar routing‑nyckel (default: message.published)
    @Value("${app.mq.routing-key:message.published}")
    private String routingKey;

    // Skapar en hållbar TopicExchange för chat‑händelser
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    // Skapar en hållbar kö som inte är auto‑delete
    @Bean
    public Queue chatQueue() {
        return new Queue(queueName, true);
    }

    // Binder kön till exchanget med specificerad routing‑nyckel
    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(routingKey);
    }
}
