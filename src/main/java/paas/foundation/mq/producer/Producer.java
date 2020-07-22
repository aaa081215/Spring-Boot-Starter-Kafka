package paas.foundation.mq.producer;

public interface Producer {

    /**
     * 同步发送消息的接口
     *
     * @param message 待发送的消息
     * @return 消息成功发送到消息队列后返回消息队列分配的id，可用于后续查询消息，排查错误
     */
    String send(ProduceMessage message);
}
