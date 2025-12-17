package com.nei10u.tip.ordersync.config;

import com.nei10u.tip.ordersync.support.ProfitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProfitProperties.class)
public class ProfitConfig {
}

