package com.aurora.iotonenet.infrastructure.pulsar.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.aurora.iotonenet.config.OneNetProperties;
import com.aurora.iotonenet.infrastructure.pulsar.auth.AESBase64Utils;
import com.aurora.iotonenet.infrastructure.pulsar.client.IoTConsumer;
import com.aurora.iotonenet.infrastructure.pulsar.handler.DeviceMessageHandler;
import com.aurora.iotonenet.infrastructure.pulsar.model.IoTMessage;
import com.aurora.iotonenet.infrastructure.pulsar.parser.OriginalMessageParser;
import com.aurora.iotonenet.infrastructure.pulsar.parser.OriginalMessageParser.ParseResult;
import io.netty.util.internal.StringUtil;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class IoTPulsarConsumer {

    private static final Logger logger = LoggerFactory.getLogger(IoTPulsarConsumer.class);
    private final OneNetProperties oneNetProperties;
    private final DeviceMessageHandler deviceMessageHandler;
    private final OriginalMessageParser originalMessageParser;

    @Value("${server.port:8082}")
    private int serverPort;

    public IoTPulsarConsumer(OneNetProperties oneNetProperties,
                             DeviceMessageHandler deviceMessageHandler,
                             OriginalMessageParser originalMessageParser) {
        this.oneNetProperties = oneNetProperties;
        this.deviceMessageHandler = deviceMessageHandler;
        this.originalMessageParser = originalMessageParser;
    }

    public void startConsuming() throws Exception {
        logger.info("HTTP服务已由Spring Boot启动在 http://127.0.0.1:{}", serverPort);
        logger.info("API接口: http://127.0.0.1:{}/api/status", serverPort);

        if (oneNetProperties.getPulsar().isAutoOpenDashboard()) {
            tryOpenDashboard();
        }

        String accessId = oneNetProperties.getPulsar().getAccessId();
        String secretKey = oneNetProperties.getPulsar().getSecretKey();
        String subscriptionName = oneNetProperties.getPulsar().getSubscriptionName();
        String brokerUrl = oneNetProperties.getPulsar().getBrokerUrl();

        validatePulsarConfig(accessId, secretKey, subscriptionName);

        logger.info("正在创建Pulsar消费者: brokerUrl={}, accessId={}, subscription={}", brokerUrl, accessId, subscriptionName);
        IoTConsumer consumer = IoTConsumer.IOTConsumerBuilder.anIOTConsumer()
                .brokerServerUrl(brokerUrl)
                .iotAccessId(accessId)
                .iotSecretKey(secretKey)
                .subscriptionName(subscriptionName)
                .iotMessageListener(message -> {
                    MessageId msgId = message.getMessageId();
                    long publishTime = message.getPublishTime();
                    String payload = new String(message.getData());
                    logger.info("【Pulsar消息到达】messageId={}, publishTime={}, payload={}", msgId, publishTime, payload);

                    IoTMessage iotMessage = JSONObject.parseObject(payload, IoTMessage.class);
                    String originalMsg = AESBase64Utils.decrypt(iotMessage.getData(), secretKey.substring(8, 24));
                    logger.info("【解密后的消息】originalMsg: {}", originalMsg);

                    try {
                        ParseResult result = originalMessageParser.parse(originalMsg);
                        if (result.getType() == OriginalMessageParser.Type.SET_REPLY) {
                            if (result.getDeviceId() == null) {
                                deviceMessageHandler.handleSetReplyByRequestIdOnly(result.getRequestId(), result.isSuccess(), result.getMessage());
                                logger.info("set_reply completed by requestId -> id={}, success={}, msg={}",
                                        result.getRequestId(), result.isSuccess(), result.getMessage());
                            } else {
                                deviceMessageHandler.handleSetReply(result.getDeviceId(), result.getRequestId(), result.isSuccess(), result.getMessage());
                                logger.info("set_reply completed -> deviceId={}, id={}, success={}, msg={}",
                                        result.getDeviceId(), result.getRequestId(), result.isSuccess(), result.getMessage());
                            }
                        } else if (result.getType() == OriginalMessageParser.Type.PROPERTY_REPORT) {
                            deviceMessageHandler.handlePropertyReport(result.getDeviceId(), result.getDeviceName(),
                                    result.getTemperature(), result.getHumidity(), result.getLight(), result.getMq2(), result.getError(), result.getLed());
                            logger.info("【设备状态已更新】deviceId={}, deviceName={}, temp={}, hum={}, light={}, mq2={}, error={}, led={}, ts={}",
                                    result.getDeviceId(), result.getDeviceName(), result.getTemperature(), result.getHumidity(),
                                    result.getLight(), result.getMq2(), result.getError(), result.getLed(), result.getTimestamp());
                        } else {
                            logger.warn("【数据异常】{}", result.getMessage());
                        }
                    } catch (Exception ex) {
                        logger.error("【消息解析失败】originalMsg={}, error={}", originalMsg, ex.getMessage(), ex);
                    }
                })
                .build();
        consumer.run();
    }

    private void validatePulsarConfig(String accessId, String secretKey, String subscriptionName) {
        if (StringUtil.isNullOrEmpty(accessId)) {
            throw new IllegalStateException("iotAccessId is null, please input iotAccessId");
        }
        if (StringUtil.isNullOrEmpty(secretKey)) {
            throw new IllegalStateException("iotSecretKey is null, please input iotSecretKey");
        }
        if (StringUtil.isNullOrEmpty(subscriptionName)) {
            throw new IllegalStateException("iotSubscriptionName is null, please input iotSubscriptionName");
        }
    }

    private void tryOpenDashboard() {
        try {
            String url = "http://127.0.0.1:" + serverPort + "/login.html";
            URI uri = new URI(url);
            logger.info("正在打开登录页面: {}", url);

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(uri);
                    logger.info("仪表盘已在浏览器中打开");
                    return;
                } catch (Exception e) {
                    logger.debug("Desktop.browse失败: {}", e.getMessage());
                }
            }

            if (tryOpenUriByCommand(url)) {
                logger.info("仪表盘已通过系统命令打开");
            } else {
                logger.info("无法自动打开浏览器，请手动访问: {}", url);
            }
        } catch (Throwable t) {
            logger.warn("Open dashboard failed: {}", t.getMessage());
        }
    }

    private static boolean tryOpenUriByCommand(String uri) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", uri);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", uri);
            } else {
                pb = new ProcessBuilder("xdg-open", uri);
            }
            pb.inheritIO();
            pb.start();
            return true;
        } catch (Throwable e) {
            logger.debug("open by command failed: {}", e.getMessage());
            return false;
        }
    }
}
