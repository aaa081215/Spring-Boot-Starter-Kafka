# Spring-Boot-Starter-Kafka
spring-boot-starter-kafka, which allows users to quickly integrated kafka or Ckfka stream processing platform in a Spring Boot project, supports failure compensation mechanisms and dead-letter messages.There will be no repeat consumption or message loss.

### Start using the spring-boot-starter
##### 1. Introduce spring-boot-starter into your project's pom.xml (You need to execute Maven Install first to ensure that the Maven repository exists in Spring-boot-starter kafka)
```java
<dependency>
    <groupId>paas.foundation</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```
##### 2. Kafka bootstrap-Server needs to be configured in application.yml

```java
paas:
  mq:
    kafka:
      bootstrap-server: xxx.xx.xx.xx:9092
```
##### 3. Start using: Producers send messages
```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import paas.foundation.mq.producer.ProduceMessage;
import paas.foundation.mq.producer.Producer;
import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {
    @Autowired
    private Producer producer;

    @GetMapping("/send")
    void sendMsg() {
        for (int i = 1; i <= 30; i++) {
            ProduceMessage message = ProduceMessage.fromString("wpp_test_01", "hello world！");
            Map<String, String> userProperties = new HashMap<>();
            userProperties.put("testHeader01", "1");
            userProperties.put("testHeader02", "2");
            message.setUserProperties(userProperties);
            producer.send(message);
        }
    }
}
```

##### 4. Consumer consumption messages
```java
import org.springframework.stereotype.Component;
import paas.foundation.mq.consumer.ConsumeMessage;
import paas.foundation.mq.consumer.MessageListener;

@Component
public class KafkaListener implements MessageListener {

    /**
     * Declare which Topic to listen on
     */
    @Override
    public String getTopic() {
        return "wpp_test_01";
    }

    @Override
    public void process(ConsumeMessage message) {
        System.out.println("The message heard is" + message.getValueAsString());
        System.out.println("The context information being listened to is " + message.getUserProperties());
    }
}
```

### Advanced Function

- Message retry

Support consumption failure retry, total retry 16 times: 10s, 30s, 1min, 2min...10min, 20min, 30min, 1h, 2h were put into the dead letter topic(
DEAD_TOPIC), waiting for manual consumption compensation.'retry-count: 2' means to retry twice
```java
paas:
  mq:
    kafka:
      bootstrap-server:  xxx.xx.xx.xx:9092
      retry-count: 2
```
- Message sending response mechanism (Acks - Config).

  0 means that the message is sent out and the success is returned;
  
  1 means after the message is sent, the leader will confirm and return successfully;
  
  -1 means represents the successful return of the message after the leader and all followers confirm.
```java
paas:
  mq:
    kafka:
      bootstrap-server:  xxx.xx.xx.xx:9092
      acks-config: 1
```
- Integrated ckfka
```java
paas:
  mq:
    kafka:
      bootstrap-server: ckafka-xxxxxx.xx-beijing.ckafka.tencentcloudmq.com:6007
      security-protocol: SASL_PLAINTEXT
      sasl-mechanism: PLAIN
      sasl-jaas-config: org.apache.kafka.common.security.plain.PlainLoginModule required username="xxxxxx#root" password="xxxxxx";
```
