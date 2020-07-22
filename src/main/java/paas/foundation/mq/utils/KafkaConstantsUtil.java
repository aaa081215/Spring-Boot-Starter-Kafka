package paas.foundation.mq.utils;


/**
 * @author wangpengpeng
 * @date 2020-07-04 14:55
 */
public class KafkaConstantsUtil {
    public static final String RETRY_TOPIC = "RETRY_TOPIC";
    public static final String DEAD_TOPIC = "DEAD_TOPIC";
    public static final String TOPIC_CAPTION = "topic";
    public static final String RETRY_COUNT = "retry_count";
    public static final Integer RETRY_10_SECOND = 10000;
    public static final Integer RETRY_30_SECOND = 30000;
    public static final Integer RETRY_1_MIN = 60000;
    public static final Integer RETRY_2_MIN = 120000;
    public static final Integer RETRY_3_MIN = 180000;
    public static final Integer RETRY_4_MIN = 240000;
    public static final Integer RETRY_5_MIN = 300000;
    public static final Integer RETRY_6_MIN = 360000;
    public static final Integer RETRY_7_MIN = 420000;
    public static final Integer RETRY_8_MIN = 480000;
    public static final Integer RETRY_9_MIN = 540000;
    public static final Integer RETRY_10_MIN = 600000;
    public static final Integer RETRY_20_MIN = 1200000;
    public static final Integer RETRY_30_MIN = 1800000;
    public static final Integer RETRY_1_H = 3600000;
    public static final Integer RETRY_2_H = 7200000;
}
