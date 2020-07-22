package paas.foundation.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

/**
 * 描述: 接收消息的上层统一数据结构.
 *
 * @author wangpengpeng
 * @date 2020-07-02 12:49
 */
public class ConsumeMessage implements Serializable {
    /**
     * 消息队列返回的当前消息的id值
     */
    @Getter
    private String messageId;

    /**
     * 消息体的二进制数组格式
     */
    private final byte[] payload;

    /**
     * 消息主题
     */
    @Getter
    private String topic;

    /**
     * 消息tag
     */
    @Getter
    private String tag;
    /**
     * 消息是否已设置为提交状态
     */
    @Getter
    private boolean committed;
    /**
     * 正常返回的情况下，消息队列框架是否自动提交消息
     */
    @Getter
    @Setter
    private boolean autoCommit = true;

    /**
     * 返回当前消息失败重试的次数.
     */
    @Getter
    @Setter
    private int reconsumeTimes;

    /**
     * userProperties存储了Ons和rabbitmq的Message的原始属性信息.
     * 直接通过key获取相应value.
     */
    @Getter
    @Setter
    private Properties userProperties;

    public ConsumeMessage(String messageId, byte[] payload) {
        this.messageId = messageId;
        this.payload = payload;
    }

    public ConsumeMessage(String messageId, byte[] payload, String topic, String tag) {
        this.messageId = messageId;
        this.payload = payload;
        this.topic = topic;
        this.tag = tag;
    }

    /**
     * 提交消息，表示该消息已处理完成
     */
    public final void commit() {
        this.committed = true;
    }

    /**
     * 读取消息内容，以byte数组形式返回
     */
    public byte[] getValueAsBytes() {
        return payload;
    }

    /**
     * 读取消息内容，以Json对象形式返回
     */
    public JSONObject getValueAsJson() {
        return (JSONObject) JSON.parse(payload);
    }

    /**
     * 读取消息内容，以对象形式返回
     */
    public <T> T getValueAsObject(Class<T> cls) {
        return JSON.parseObject(payload, cls);
    }

    /**
     * 回读取消息内容，以字符串形式返
     */
    public String getValueAsString() {
        return new String(payload, Charset.defaultCharset());
    }
}
