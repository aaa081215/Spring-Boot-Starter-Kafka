package paas.foundation.mq.producer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.shade.org.apache.commons.codec.Charsets;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;


@Data
public class ProduceMessage {

    /**
     * 消息所属的主题
     */
    private final String topic;
    /**
     * 消息体的二进制数据
     */
    private final byte[] payload;

    /**
     * 消息的tag，接收端可用于进行消息的二级分类，对发送端无影响。
     * 一般情况下接收消息的一端会接收topic相同的消息，当接收消息设置了相同的tag的时候仅接收同一主题下相同tag的消息
     */
    private String tag;
    /**
     * 消息需要延迟发送的时间。
     * 默认情况下消息立即发送，当设置了该值后消息会在间隔给定的时间后再发送。
     * 如果没有设置该值，且没有设置{@link #getAtTime()}，则消息会立即送达接收端
     */
    private Integer delayTime;
    /**
     * 设置一个时间，表示消息进入消息队列后在指定的时间才会被推送给接收端。
     * 该值的优先级低于 {@link #getDelayTime()} 属性
     */
    private Long atTime;

    /**
     * 如果此值不为NULL，则发送有序消息，同一个值下的消息会保证有序
     */
    private String shardingKey = "";

    /**
     * 消息队列附加的额外的属性。
     * 不解析消息体就能看到某些特殊的信息（例如租户Id，项目id等），可用于查询消息历史时过滤使用。
     */
    private Map<String, String> userProperties = new HashMap<>();

    private ProduceMessage(String topic, byte[] data) {
        this.topic = topic;
        this.payload = data;
    }

    private ProduceMessage(String topic, String tag, byte[] data) {
        this.topic = topic;
        this.tag = tag;
        this.payload = data;
    }

    public static ProduceMessage fromBinary(String topic, byte[] data) {
        return new ProduceMessage(topic, data);
    }

    public static ProduceMessage fromString(String topic, String data) {
        byte[] binary = data.getBytes(Charsets.UTF_8);
        return new ProduceMessage(topic, binary);
    }

    public static ProduceMessage fromString(String topic, String tag, String data) {
        byte[] binary = data.getBytes(Charsets.UTF_8);
        return new ProduceMessage(topic, tag, binary);
    }

    public static ProduceMessage fromJSON(String topic, JSONObject data) {
        byte[] binary = JSON.toJSONBytes(data);
        return fromBinary(topic, binary);
    }

    public static ProduceMessage fromObject(String topic, Object obj) {
        byte[] binary = JSON.toJSONBytes(obj);
        return fromBinary(topic, binary);
    }
}
