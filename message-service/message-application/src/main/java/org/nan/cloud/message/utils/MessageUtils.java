package org.nan.cloud.message.utils;

public class MessageUtils {

    private MessageUtils() {}

    /**
     * 生成消息唯一ID
     *
     * @return 消息ID
     */
    public
    static String generateMessageId() {
        return "msg-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}
