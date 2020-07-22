package paas.foundation.mq.consumer.internal.kafka;

import paas.foundation.autoconfigure.mq.kafka.KafkaProperties;
import paas.foundation.mq.consumer.MessageListener;
import paas.foundation.mq.exception.MessageQueueException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static paas.foundation.mq.utils.KafkaConstantsUtil.DEAD_TOPIC;
import static paas.foundation.mq.utils.KafkaConstantsUtil.RETRY_TOPIC;

/**
 * 描述: 抽象消费者
 *
 * @author wangpengpeng
 * @date 2020-07-04 14:55
 */
@Slf4j
abstract class AbstractKafkaConsumer {

    /**
     * 主题与消息监听Map结合
     */
    Map<String, MessageListener> topicMessageListenerListConcurrentHashMap = new ConcurrentHashMap<>();
    /**
     * 主题与消费者Map结合
     */
    Map<String, KafkaConsumer<String, byte[]>> topicKafkaConsumerConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * 重试
     */
    protected Properties properties;
    protected KafkaProducer<String, byte[]> retryProducer;
    private Integer propertiesRetryCount;
    private KafkaConsumer<String, byte[]> retryConsumer;

    /**
     * kafka所有topic集合
     */
    Set<String> topicSet = new CopyOnWriteArraySet<>();

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private KafkaProperties kafkaProperties;

    /**
     * 初始化生产者
     */
    void init() throws MessageQueueException {
        // 1.初始化kafka属性
        initKafkaProperties();
        // 2.做TOPIC校验
        topicCreateAndGet();
        // 3.初始化kafka消费者
        initKafkaConsumer();
        // 4.初始化kafka重试
        initKafkaRetryConsumer();
    }

    /**
     * 初始化kafka属性
     */
    private void initKafkaProperties() {
        properties = new Properties();
        // getBootstrapServer（必填 ）
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServer());
        // 公网（非必填 ）
        if (kafkaProperties.getSecurityProtocol() != null) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol());
        }
        if (kafkaProperties.getSaslMechanism() != null) {
            properties.put("sasl.mechanism", kafkaProperties.getSaslMechanism());
        }
        if (kafkaProperties.getSaslJaasConfig() != null) {
            properties.put("sasl.jaas.config", kafkaProperties.getSaslJaasConfig());
        }
        properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        properties.put("enable.auto.commit", "false");
        //序列化类
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
    }

    /**
     * 创建重试，死信topic并获取所有topic列表
     */
    private void topicCreateAndGet() throws MessageQueueException {
        AdminClient adminClient = KafkaAdminClient.create(properties);
        createTopics(adminClient, RETRY_TOPIC);
        createTopics(adminClient, DEAD_TOPIC);
        try {
            topicSet = getAllTopic(adminClient);
            log.info("获取kafka的topic列表为：{}", topicSet);
        } catch (Exception e) {
            log.error("获取kafka的topic列表失败 {}", e.getMessage());
            throw new MessageQueueException("获取kafka的topic列表失败");
        }
    }

    /**
     * 初始化kafka消费者
     */
    private void initKafkaConsumer() throws MessageQueueException {
        Map<String, MessageListener> messageListenerBeans = applicationContext.getBeansOfType(MessageListener.class);
        Collection<MessageListener> messageListenerCollection = messageListenerBeans.values();

        if (!messageListenerCollection.isEmpty()) {
            for (MessageListener messageListener : messageListenerCollection) {
                String topic = messageListener.getTopic();
                if (topicSet.contains(topic)) {
                    properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, messageListener.getTopic());
                    KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(properties);
                    consumer.subscribe(Collections.singleton(messageListener.getTopic()));
                    this.topicMessageListenerListConcurrentHashMap.put(messageListener.getTopic(), messageListener);
                    this.topicKafkaConsumerConcurrentHashMap.put(messageListener.getTopic(), consumer);
                } else {
                    log.error("kafka实例中不存在topic:{},请删除您在实例中创建该topic，或删除该topic对应的listener", topic);
                    throw new MessageQueueException("kafka实例中不存在topid:" + topic + ",请删除您在实例中创建该topic，或删除该topic对应的listener");
                }
            }
        }
    }

    /**
     * 初始化kafka重试
     */
    private void initKafkaRetryConsumer() throws MessageQueueException {
        propertiesRetryCount = kafkaProperties.getRetryCount();
        if (propertiesRetryCount != null && propertiesRetryCount > 0) {
            if (propertiesRetryCount > 16) {
                throw new MessageQueueException("retry-count应小于等于16");
            }
            if (topicSet.contains(RETRY_TOPIC) && topicSet.contains(DEAD_TOPIC)) {
                initRetryProducer();
                initRetryConsumer();
            } else {
                throw new MessageQueueException("kafka实例中不存在RETRY_TOPIC或DEAD_TOPIC，请手动创建");
            }
        } else {
            log.info("消息不进行失败重试");
        }
    }

    /**
     * 初始化重试生产者
     */
    private void initRetryProducer() {
        this.retryProducer = new org.apache.kafka.clients.producer.KafkaProducer<>(properties);
    }

    /**
     * 初始化重试消费者
     */
    private void initRetryConsumer() {
        properties.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, RETRY_TOPIC);
        retryConsumer = new KafkaConsumer<>(properties);
        retryConsumer.subscribe(Collections.singleton(RETRY_TOPIC));
        ThreadFactory namedThreadFactory = new CustomizableThreadFactory("retry-thread-pool");
        ExecutorService singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.submit(new KafkaRetryConsumerServer(this.retryProducer, retryConsumer, propertiesRetryCount));
        singleThreadPool.shutdown();
    }

    /**
     * 获取所有的topic列表
     */
    private static Set<String> getAllTopic(AdminClient client) throws InterruptedException, ExecutionException {
        return client.listTopics().listings().get().stream().map(TopicListing::name).collect(Collectors.toSet());
    }

    /**
     * 创建Topic
     * 腾讯云无法创建
     */
    private static void createTopics(AdminClient adminClient, String name) {
        NewTopic newTopic = new NewTopic(name, 1, (short) 1);
        Collection<NewTopic> newTopicList = new ArrayList<>();
        newTopicList.add(newTopic);
        adminClient.createTopics(newTopicList);
    }
}
