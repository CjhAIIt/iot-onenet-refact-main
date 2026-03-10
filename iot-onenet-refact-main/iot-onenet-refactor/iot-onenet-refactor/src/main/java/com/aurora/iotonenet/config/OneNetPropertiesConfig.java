package com.aurora.iotonenet.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OneNetProperties.class, AppProperties.class})
public class OneNetPropertiesConfig {
}
