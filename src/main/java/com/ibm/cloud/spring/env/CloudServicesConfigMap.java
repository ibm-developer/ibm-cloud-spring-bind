package com.ibm.cloud.spring.env;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class CloudServicesConfigMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudServicesConfigMap.class);
    private static final String MAPPINGS_JSON = "/mappings.json";
    private static final String VCAP_SERVICES = "VCAP_SERVICES";

    JsonNode config = null;            //configuration to be using
    private final ConcurrentMap<String, DocumentContext> resourceCache = new ConcurrentHashMap<>();    //used to cache resources loaded during processing

    @Autowired
    ApplicationContext appContext;

    static class SingletonHelper {
        static CloudServicesConfigMap MAPPINGS;

        static {
            MAPPINGS = new CloudServicesConfigMap();
            MAPPINGS.config = MAPPINGS.getJson(MAPPINGS_JSON);
        }
    }

    /**
     * Create a cloud services mapping object from mappings.json
     *
     * @return the configured service mapper
     */
    static CloudServicesConfigMap fromMappings() {
        return SingletonHelper.MAPPINGS;
    }

    JsonNode getJson(String path) {
        LOGGER.debug("getJson() for " + path);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mappings = null;
        try {
            Resource resource = new ClassPathResource(path);
            if (resource.exists()) {
                InputStream fstream = resource.getInputStream();
                if (fstream != null) {
                    mappings = mapper.readTree(fstream);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Unexpected exception getting ObjectMapper for mappings.json: " + e);
            throw new CloudServicesException("Unexpected exception getting ObjectMapper for mappings.json", e);
        }
        LOGGER.debug("getMappings() returned: " + mappings);
        if (mappings == null) {
            LOGGER.warn("Mapping resolution failed : No configuration was found at " + path);
        }
        return mappings;
    }

    /**
     * Get the first value found from the provided searchPatterns, which will be
     * processed in the order provided.
     *
     * @param name The name to be extracted from the "searchPatterns" containing
     *             an array of Strings. Each String is a search pattern
     *             with format "src:target"
     * @return The value specified by the "src:target" or null if not found
     */
     String getValue(String name) {
        if (config == null) {
            return null;    //config wasn't initialised for some reason, so cannot resolve anything
        }
        String value = null;
        String keySegment[] = parseOnfirst(name, ".");
        if (!keySegment[0].isEmpty() && !keySegment[1].isEmpty()) {
            JsonNode node = config.get(keySegment[0]);
            if (node == null || node.isNull()) {
                return null;        // 1st segment could not be located
            }
            node = node.get(keySegment[1]);
            if (node == null || node.isNull()) {
                return null;        // 2nd segment could not be located
            }
            ArrayNode array = (ArrayNode) node.get("searchPatterns");
            if (array.isArray()) {
                for (final JsonNode entryNode : array) {
                    String entry = entryNode.asText();
                    LOGGER.debug("entryNode " + entryNode);
                    String token[] = parseOnfirst(entry, ":");
                    LOGGER.debug("tokens " + token[0] + " , " + token[1]);
                    if (!token[0].isEmpty() && !token[1].isEmpty()) {
                        switch (token[0]) {
                            case "cloudfoundry":
                                value = getCloudFoundryValue(token[1]);
                                break;
                            case "env":
                                value = getEnvValue(token[1]);
                                break;
                            case "file":
                                value = getFileValue(token[1]);
                                break;
                            default:
                                LOGGER.warn("Unknown protocol in searchPatterns : " + token[0]);
                                break;
                        }
                    }
                    if (value != null) {
                        break;
                    }
                }
            } else {
                LOGGER.warn("search patterns in mapping.json is NOT an array, values will not be resolved");
            }
        }
        return value;
    }

    private String[] parseOnfirst(String entry, String delimiter) {
        String token[] = {"", ""};
        int i = entry.indexOf(delimiter);
        if (i > 1) {
            token[0] = entry.substring(0, i).trim();
            token[1] = entry.substring(i + 1).trim();
        }
        return token;
    }

    private String getJsonValue(String jsonPath, String json) {
        String value = null;
        if (jsonPath != null && json != null) {
            value = JsonPath.parse(json).read(jsonPath);
        }
        return value;
    }

    // Search pattern resolvers
    private String getCloudFoundryValue(String target) {
        if (!target.startsWith("$"))
            return null;
        return getJsonValue(target, getEnvironmentValue(VCAP_SERVICES));
    }

    private String getEnvValue(String target) {
        String value = null;
        if (target.contains(":")) {
            String token[] = parseOnfirst(target, ":");
            LOGGER.debug("envtokens " + token[0] + " , " + token[1]);
            if (!token[0].isEmpty() && !token[1].isEmpty() && token[1].startsWith("$")) {
                value = getJsonValue(token[1], getEnvironmentValue(token[0]));
            }
        } else {
            value = getEnvironmentValue(target);
        }
        if (value != null) {
            value = sanitiseString(value);
        }
        return value;
    }

    private String getFileValue(String target) {
        String value = null;
        if (target.contains(":")) {
            String token[] = parseOnfirst(target, ":");
            if (!token[0].isEmpty() && !token[1].isEmpty() && token[1].startsWith("$")) {
                try {
                    String path = token[0];
                    DocumentContext context = resourceCache.computeIfAbsent(path, filePath -> getJsonStringFromFile(filePath));
                    value = context.read(token[1]);
                } catch (PathNotFoundException e) {
                    return null;    //no data matching the specified json path
                }
            }
        } else {
            // if no location within the file has been specified then
            // assume that the value == the first line of the file contents
            // Relative path means it's a classpath resource
            String path = target.startsWith("/") ? "file:" + target.trim() : "classpath:" + target.trim();
            LOGGER.debug("Looking for resource : " + path);
            try {
                Resource resource = appContext.getResource(path);
                if (resource.exists()) {
                    InputStream fstream = resource.getInputStream();
                    if (fstream != null) {
                        InputStreamReader isReader = new InputStreamReader(fstream);
                        BufferedReader reader = new BufferedReader(isReader);
                        value = reader.readLine();
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Unexpected exception getting ObjectMapper for mappings.json: " + e);
                throw new CloudServicesException("Unexpected exception getting ObjectMapper for mappings.json", e);
            }
        }
        return value;
    }

    //end search pattern resolvers

    private DocumentContext getJsonStringFromFile(String filePath) {
        String json = null;
        if (filePath != null && !filePath.isEmpty()) {
            if (!filePath.startsWith("/")) {
                // Relative path means it's a classpath resource
                LOGGER.debug("Looking for classpath resource : " + filePath);
                JsonNode node = getJson(filePath);
                if (node != null) {
                    json = node.toString();
                    LOGGER.debug("Class path json : " + json);
                }
            } else {
                // look for the file specified
                try {
                    json = new String(Files.readAllBytes(Paths.get(filePath)));
                } catch (Exception e) {
                    LOGGER.debug("Unexpected exception reading JSON string from file: " + e);
                }
            }
        }
        if (json == null) {
            return JsonPath.parse("{}");    //parse an empty object and set that for the context if the file cannot be loaded for some reason
        }
        return JsonPath.parse(json);
    }

    private String sanitiseString(String data) throws CloudServicesException {
        if (data == null || data.isEmpty()) {
            throw new CloudServicesException("Invalid string [" + data + "]");
        }
        char first = data.charAt(0);
        char last = data.charAt(data.length() - 1);
        if ((first == '"' || first == '\'') && (first == last)) {
            return data.substring(1, data.length() - 1);
        }
        return data;
    }

    private static String getEnvironmentValue(String key) {
        String value = System.getenv(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }
}

