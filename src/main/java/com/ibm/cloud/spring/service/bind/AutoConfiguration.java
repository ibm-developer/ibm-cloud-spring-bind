package com.ibm.cloud.spring.service.bind;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfiguration {

    @Bean
    public ServiceMappings getServiceMappings() {
        return new ServiceMappings();
    }
}
