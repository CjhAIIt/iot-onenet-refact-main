package com.aurora.iotonenet.bootstrap;

import com.aurora.iotonenet.infrastructure.pulsar.consumer.IoTPulsarConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PulsarConsumerBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PulsarConsumerBootstrap.class);
    private final IoTPulsarConsumer ioTPulsarConsumer;

    public PulsarConsumerBootstrap(IoTPulsarConsumer ioTPulsarConsumer) {
        this.ioTPulsarConsumer = ioTPulsarConsumer;
    }

    @Override
    public void run(String... args) {
        logger.info("Spring Boot 启动完成，正在初始化 Pulsar 消费者...");
        Thread worker = new Thread(() -> {
            try {
                ioTPulsarConsumer.startConsuming();
            } catch (Exception e) {
                logger.error("Pulsar 消费者启动失败", e);
            }
        }, "pulsar-consumer-thread");
        worker.setDaemon(true);
        worker.start();
    }
}
