package com.ibm.cloud.spring.env;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.Assert.assertEquals;

/**
 *  This class directly tests CloudServicesPropertySource.
 *  CloudServicesConfigMap is completely, though indirectly,
 *  exercised by these tests.
 */
public class CloudServicesConfigTest {

    String VCAP_SERVICES = "{\"cloudantNoSQLDB\":[{\"credentials\":{\"username\":\"VCAP_SERVICES-username\",\"password\":\"VCAP_SERVICES-password\",\"host\":\"VCAP_SERVICES.cloudant.com\",\"port\":999,\"url\":\"https://VCAP_SERVICES.cloudant.com\"},\"syslog_drain_url\":null,\"volume_mounts\":[],\"label\":\"cloudantNoSQLDB\",\"provider\":null,\"plan\":\"Lite\",\"name\":\"VCAP_SERVICES-cloudantno-1234567890\",\"tags\":[\"data_management\",\"ibm_created\",\"lite\",\"ibm_dedicated_public\"]}]}";

    String CLOUDANT_CONFIG_JSON = "{\"cloudant_username\":\"env-json-username\"}";

    private final CloudServicesEnvironmentPostProcessor initializer =
            new CloudServicesEnvironmentPostProcessor();

    private final ConfigurableApplicationContext context =
            new AnnotationConfigApplicationContext();

    @Before
    public void setUp() {
        initializer.postProcessEnvironment(this.context.getEnvironment(), null);
        System.setProperty("is_unit_test", "true");
    }

    @Test
    public void getValueCF() {
        System.setProperty("VCAP_SERVICES", VCAP_SERVICES);
        String userName = context.getEnvironment().getProperty("cloudant.username");
        System.clearProperty("VCAP_SERVICES");
        assertEquals(userName, "VCAP_SERVICES-username");
    }

    @Test
    public void getValueEnv() {
        System.setProperty("cloudant_username", "env-username");
        String userName = context.getEnvironment().getProperty("cloudant.username");
        System.clearProperty("cloudant_username");
        assertEquals(userName, "env-username");
    }

    @Test
    public void getValueEnvJSON() {
        System.setProperty("cloudant_config", CLOUDANT_CONFIG_JSON);
        String userName = context.getEnvironment().getProperty("cloudant.username");
        System.clearProperty("cloudant_config");
        assertEquals(userName, "env-json-username");
    }

    @Test
    public void getValueFile() {
        String userName = context.getEnvironment().getProperty("cloudant.username");
        assertEquals(userName, "file-json-username");
    }

    @Test
    public void getValueFileJSON() {
        String url = context.getEnvironment().getProperty("cloudant.url");
        assertEquals(url, "https://file-url.cloudant.com");
    }

    @Test
    public void getValueApplicationProperties() {
        TestPropertySourceUtils.addPropertiesFilesToEnvironment(context, "/application.properties");
        String blah = context.getEnvironment().getProperty("blah.blah");
        assertEquals(blah, "The quick brown fox jumps over the lazy dog.");
    }
}