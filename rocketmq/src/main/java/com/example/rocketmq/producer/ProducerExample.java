package com.example.rocketmq.producer;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

/**
 * RocketMQ官网(https://rocketmq.apache.org/docs/%e5%bf%ab%e9%80%9f%e5%85%a5%e9%97%a8/02quickstart/)提供的Demo
 */
public class ProducerExample {

    public static void main(String[] args) throws ClientException {
        // 接入点地址，需要设置成代理的端口和列表
        String endpoint = "localhost:8081";
        // 消息发送目标的主题，需要提前在MQ中创建
        String topic = "TestTopic";

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoint);
        ClientConfiguration configuration = builder.build();
        // 初始化producer时需要设置通信配置以及绑定topic
        Producer producer = provider.newProducerBuilder()
                .setTopics(topic)
                .setClientConfiguration(configuration)
                .build();
        // 普通消息发送
        Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setKeys("messageKey")
                .setTag("messageTag")
                .build();
        SendReceipt send = producer.send(message);
        System.out.println("messageID：" + send.getMessageId());
    }
}
