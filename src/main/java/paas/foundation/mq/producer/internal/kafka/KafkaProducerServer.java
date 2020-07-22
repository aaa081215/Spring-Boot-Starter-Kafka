package paas.foundation.mq.producer.internal.kafka;

import paas.foundation.mq.producer.ProduceMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.util.Iterator;
import java.util.Map;


/**
 * 描述: 抽象消费者
 *
 * @author wangpengpeng
 * @create 2020-07-05 14:34
 */
@Slf4j
public class KafkaProducerServer extends AbstractKafkaMqProducer {

    @Override
    public String send(ProduceMessage message) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(message.getTopic(), null, null, null, message.getPayload(), getUserProperties(message));
        try {
            RecordMetadata recordMetadata = producer.send(producerRecord).get();
            return String.valueOf(recordMetadata);
        } catch (Exception e) {
            log.error("发送失败 {}", e.getMessage());
        }
        return null;
    }

    /**
     * 拼装header头
     *
     * @param message ProduceMessage
     * @return RecordHeaders
     */
    private RecordHeaders getUserProperties(ProduceMessage message) {
        RecordHeader recordHeaders[] = null;
        Map<String, String> messageUserProperties = message.getUserProperties();
        int messageUserPropertiesSize = messageUserProperties.size();

        if (messageUserPropertiesSize > 0) {
            recordHeaders = new RecordHeader[messageUserPropertiesSize];
            Iterator<Map.Entry<String, String>> messageUserPropertiesIterator = messageUserProperties.entrySet().iterator();
            while (messageUserPropertiesIterator.hasNext()) {
                Map.Entry<String, String> entry = messageUserPropertiesIterator.next();
                recordHeaders[--messageUserPropertiesSize] = new RecordHeader(entry.getKey(), entry.getValue().getBytes());
            }
        }
        return new RecordHeaders(recordHeaders);
    }

}
