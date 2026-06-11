package com.eventhub.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrderMessagingProperties.class)
public class OrderMessagingConfig {

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(OrderMessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange orderDelayExchange() {
        return new DirectExchange(OrderMessagingTopology.DELAY_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(OrderMessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue orderTimeoutDelayQueue() {
        return QueueBuilder.durable(OrderMessagingTopology.TIMEOUT_DELAY_QUEUE)
                .deadLetterExchange(OrderMessagingTopology.EVENTS_EXCHANGE)
                .deadLetterRoutingKey(OrderMessagingTopology.TIMEOUT_KEY)
                .build();
    }

    @Bean
    Queue orderTimeoutQueue() {
        return QueueBuilder.durable(OrderMessagingTopology.TIMEOUT_QUEUE)
                .deadLetterExchange(OrderMessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(OrderMessagingTopology.TIMEOUT_DEAD_KEY)
                .build();
    }

    @Bean
    Queue orderPaidQueue() {
        return QueueBuilder.durable(OrderMessagingTopology.PAID_QUEUE)
                .deadLetterExchange(OrderMessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(OrderMessagingTopology.PAID_DEAD_KEY)
                .build();
    }

    @Bean
    Queue orderDeadQueue() {
        return QueueBuilder.durable(OrderMessagingTopology.DEAD_QUEUE).build();
    }

    @Bean
    Binding orderTimeoutDelayBinding(Queue orderTimeoutDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderTimeoutDelayQueue)
                .to(orderDelayExchange)
                .with(OrderMessagingTopology.TIMEOUT_DELAY_KEY);
    }

    @Bean
    Binding orderTimeoutBinding(Queue orderTimeoutQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderEventsExchange).with(OrderMessagingTopology.TIMEOUT_KEY);
    }

    @Bean
    Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderEventsExchange).with(OrderMessagingTopology.PAID_KEY);
    }

    @Bean
    Binding orderDeadBinding(Queue orderDeadQueue, TopicExchange orderDeadLetterExchange) {
        return BindingBuilder.bind(orderDeadQueue).to(orderDeadLetterExchange).with("order.*.dead");
    }

    @Bean
    Jackson2JsonMessageConverter orderMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory orderRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter orderMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(orderMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
