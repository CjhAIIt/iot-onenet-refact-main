package com.aurora.iotonenet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class IotOnenetApplication {

    private static final Logger logger = LoggerFactory.getLogger(IotOnenetApplication.class);

    public static void main(String[] args) {
        logger.info("正在启动 IoT OneNET 重构版应用...");
        SpringApplication.run(IotOnenetApplication.class, args);
    }
}
