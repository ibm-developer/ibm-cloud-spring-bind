package com.ibm.cloud.spring.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

@Order(ConfigFileApplicationListener.DEFAULT_ORDER - 1)
public class CloudServicesEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        environment.getPropertySources().addFirst(new CloudServicesPropertySource("CloudServicesConfigMap"));
    }
}
