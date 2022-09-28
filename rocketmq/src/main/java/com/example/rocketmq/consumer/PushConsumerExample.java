package com.example.rocketmq.consumer;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * RocketMQ官网(https://rocketmq.apache.org/docs/%e5%bf%ab%e9%80%9f%e5%85%a5%e9%97%a8/02quickstart/)提供的Demo
 */
public class PushConsumerExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushConsumerExample.class);

    public PushConsumerExample() {
    }

    public static void main(String[] args) {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        String endPoint = "localhost:8081";
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(endPoint)
                .build();
        // 订阅消息的过滤规则，表示订阅所有的Tag消息
        String tag = "*";
        FilterExpression filterExpression = new FilterExpression(tag, FilterExpressionType.TAG);
        // 为消费者指定所属的消费组信息，Group需要提前创建
        String consumerGroup = "Your ConsumerGroup";
        // 指定需要订阅那个目标topic，topic需要提前创建
        String topic = "TestTopic";
        // 初始化PushConsumer,需要绑定消费组，通信参数以及订阅关系
        try {
            PushConsumer build = provider.newPushConsumerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setConsumerGroup(consumerGroup)
                    .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
                    .setMessageListener(messageView -> {
                        // 处理消息并且返回消费结果
                        LOGGER.info("Consumer message！！！");
                        return ConsumeResult.SUCCESS;
                    })
                    .build();
            Thread.sleep(Long.MAX_VALUE);
            build.close();
        } catch (ClientException e) {
            LOGGER.error(e.toString());
        } catch (InterruptedException e) {
            LOGGER.error(e.toString());
        } catch (IOException e) {
            LOGGER.error(e.toString());
        }

    }
}
