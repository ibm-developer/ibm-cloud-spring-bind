package com.ibm.cloud.spring.service.bind;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * Bean that allows access to values set in the generated mappings.json file.
 * Once injected into a class it can be used directly to resolve values via
 * the mapBean method or indirectly with the use of property injections using
 * the @Value annotation.
 *
 */
@Component
public class ServiceMappings {

    @Autowired
    private ConfigurableEnvironment env;
    
    private Mappings mappings;
    
    @PostConstruct
    public void init() {
        //registers the mapping bean as a property source so that it works with @Value
        mappings = new Mappings("CloudServices");
        env.getPropertySources().addFirst(mappings);
    }

    public String getValue(String name) {
        return (String) mappings.getProperty(name);
    }

}
