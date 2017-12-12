package com.ibm.cloud.spring.service.bind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;


public class Mappings extends PropertySource<Object> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(CloudServices.class);
    
    private CloudServices mappings;
    
    public Mappings() {
        super("CloudServices");
        init();
    }
    
    public Mappings(String name) {
        super(name);
        init();
    }

    public Mappings(String name, String source) {
        super(name, source);
        init();
    }

    private void init() {
        try {
            mappings = CloudServices.fromMappings();
        } catch (CloudServicesException e) {
            LOGGER.warn("Error reading mappings file", e);
            mappings = null;
        }
    }
    
    @Override
    public Object getProperty(String name) {
        if(mappings != null) {
            return mappings.getValue(name);
        }
        return null;
    }
    
}

