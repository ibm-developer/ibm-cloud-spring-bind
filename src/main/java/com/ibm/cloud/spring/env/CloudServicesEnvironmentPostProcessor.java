package com.ibm.cloud.spring.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class CloudServicesEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        environment.getPropertySources().addFirst(new CloudServicesPropertySource("CloudServicesConfigMap"));
    }

}
