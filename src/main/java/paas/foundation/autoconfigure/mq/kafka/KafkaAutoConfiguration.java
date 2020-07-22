package paas.foundation.autoconfigure.mq.kafka;

import paas.foundation.mq.consumer.internal.kafka.KafkaConsumerServer;
import paas.foundation.mq.producer.internal.kafka.KafkaProducerServer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 描述: Spring Boot Starter初始化
 * 1. 将EnableConfigurationProperties 加载到Spring上下文的容器中
 * 2. 当配置文件存在“paas.mq.kafka.bootstrap-server”时新建对象
 *
 * @author wangpengpeng
 * @date 2020-07-02 12:49
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnClass({KafkaProducerServer.class, KafkaConsumer.class})
@EnableScheduling
public class KafkaAutoConfiguration {

    @ConditionalOnProperty("paas.mq.kafka.bootstrap-server")
    @Bean(initMethod = "init")
    public KafkaProducerServer kafkaProducerServer() {
        return new KafkaProducerServer();
    }

    @ConditionalOnProperty("paas.mq.kafka.bootstrap-server")
    @Bean(initMethod = "init")
    public KafkaConsumerServer kafkaConsumerServer() {
        return new KafkaConsumerServer();
    }
}
