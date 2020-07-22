package paas.foundation.mq.consumer.internal.kafka;

import lombok.extern.slf4j.Slf4j;
import paas.foundation.mq.consumer.ConsumeMessage;
import paas.foundation.mq.utils.KafkaConstantsUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

import static paas.foundation.mq.utils.KafkaConstantsUtil.RETRY_TOPIC;
import static paas.foundation.mq.utils.KafkaConstantsUtil.TOPIC_CAPTION;


/**
 * 描述: 真正消费者
 *
 * @author wangpengpeng
 * @date 2020-07-05 14:55
 */
@Slf4j
@EnableScheduling
public class KafkaConsumerServer extends AbstractKafkaConsumer {

    @Scheduled(fixedRate = 1)
    public void trigger() {
        this.topicKafkaConsumerConcurrentHashMap.forEach((itemTopic, itemKafkaConsumer) -> {

            Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<TopicPartition, OffsetAndMetadata>();
            ConsumerRecords<String, byte[]> records = itemKafkaConsumer.poll(1000);
            if (records.count() > 0) {
                log.info("拉取：{}条记录", records.count());
            }
            for (ConsumerRecord<String, byte[]> record : records) {
                // 解析kafka header
                RecordHeaders recordHeaders = (RecordHeaders) record.headers();
                Properties properties = getProperties(recordHeaders);
                // 解析kafka 转为consumeMessage
                ConsumeMessage consumeMessage = new ConsumeMessage(record.partition() + "@ " + record.offset(), String.valueOf(record.value()).getBytes(), record.topic(), null);
                consumeMessage.setUserProperties(properties);
                log.info("消费消息：timestamp ={}, topic={}, partition={}, offset={}, value={}, properties={}\n", record.timestamp(), record.topic(), record.partition(), record.offset(), record.value(), consumeMessage.getUserProperties());

                try {
                    this.topicMessageListenerListConcurrentHashMap.get(itemTopic).process(consumeMessage);
                    currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1, "no metadata"));
                    itemKafkaConsumer.commitSync(currentOffsets);
                } catch (Exception e) {
                    currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1, "no metadata"));
                    itemKafkaConsumer.commitSync(currentOffsets);
                    log.error("业务异常消费失败：timestamp ={}, topic={}, partition={}, offset={}, value={}, properties={}\n", record.timestamp(), record.topic(), record.partition(), record.offset(), record.value(), consumeMessage.getUserProperties());
                    if (retryProducer != null) {
                        int retryCount = Integer.parseInt(properties.getOrDefault(KafkaConstantsUtil.RETRY_COUNT, "0").toString());
                        recordHeaders.add(KafkaConstantsUtil.RETRY_COUNT, String.valueOf(retryCount + 1).getBytes());
                        recordHeaders.add(TOPIC_CAPTION, String.valueOf(record.topic()).getBytes());
                        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(RETRY_TOPIC, null, null, null, consumeMessage.getValueAsBytes(), recordHeaders);
                        retryProducer.send(producerRecord);
                    }
                }
            }
            itemKafkaConsumer.commitAsync();
        });
    }

    /**
     * 解析kafka recordHeaders为Properties
     *
     * @param recordHeaders recordHeaders
     * @return Properties
     */
    public static Properties getProperties(RecordHeaders recordHeaders) {
        Iterator<Header> headerIterator = recordHeaders.iterator();
        Properties properties = new Properties();
        while (headerIterator.hasNext()) {
            Header header = headerIterator.next();
            properties.setProperty(header.key(), new String(header.value()));
        }
        return properties;
    }
}
