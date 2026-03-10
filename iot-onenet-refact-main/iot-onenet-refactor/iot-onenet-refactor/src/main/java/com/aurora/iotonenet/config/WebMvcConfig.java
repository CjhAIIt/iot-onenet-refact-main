package com.aurora.iotonenet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/console/index.html");
        registry.addViewController("/console").setViewName("forward:/console/index.html");
        registry.addViewController("/console/").setViewName("forward:/console/index.html");
    }
}
