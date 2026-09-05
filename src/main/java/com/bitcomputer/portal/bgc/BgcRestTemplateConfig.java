package com.bitcomputer.portal.bgc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class BgcRestTemplateConfig {

    @Bean
    public RestTemplate bgcRestTemplate(RestTemplateBuilder builder, @Value("${bgc.timeout-ms}") long timeoutMs) {
        return builder
            .setConnectTimeout(Duration.ofMillis(timeoutMs))
            .setReadTimeout(Duration.ofMillis(timeoutMs))
            .build();
    }
}
