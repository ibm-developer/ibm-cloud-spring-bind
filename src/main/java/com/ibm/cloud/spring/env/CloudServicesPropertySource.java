package com.ibm.cloud.spring.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

class CloudServicesPropertySource extends PropertySource<Object> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(CloudServicesConfigMap.class);
    
    private CloudServicesConfigMap configMap;
    
    public CloudServicesPropertySource() {
        super("CloudServicesConfigMap");
        init();
    }
    
    public CloudServicesPropertySource(String name) {
        super(name);
        init();
    }

    public CloudServicesPropertySource(String name, String source) {
        super(name, source);
        init();
    }

    private void init() {
        try {
            configMap = CloudServicesConfigMap.fromMappings();
        } catch (CloudServicesException e) {
            LOGGER.warn("Error reading configMap file", e);
            configMap = null;
        }
    }
    
    @Override
    public Object getProperty(String name) {
        if(configMap != null) {
            return configMap.getValue(name);
        }
        return null;
    }
}

