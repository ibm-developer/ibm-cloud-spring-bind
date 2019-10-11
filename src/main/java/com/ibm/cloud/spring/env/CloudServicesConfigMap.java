package com.ibm.cloud.spring.env;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
    private static final String VCAP_SERVICES = "VCAP_SERVICES";
    private static String mappingsFile = "/mappings.json";
    private static CloudServicesConfigMap instance;
    private static JsonNode config = null;            //configuration to be using
    private final ConcurrentMap<String, DocumentContext> resourceCache = new ConcurrentHashMap<>();    //used to cache resources loaded during processing
    private int mappingsVersion = 1;

    @Autowired
    ApplicationContext appContext;

    /**
     * Create a CloudServicesConfigMap from the map file
     *
     * @return the configured service mapper
     */
    static synchronized CloudServicesConfigMap getInstance() {
        if (instance == null) {
            instance = new CloudServicesConfigMap();
            config = instance.getJson(mappingsFile);
        }
        return instance;
    }

    /**
     * Create a CloudServicesConfigMap from the given map file
     *
     * @param mapFile The name of the mapping file
     * @return The configured service mapper
     **/
    static synchronized CloudServicesConfigMap getInstance(String mapFile) {
        mappingsFile = mapFile;
        instance = null;        // force refresh
        return getInstance();
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
        IntNode versionNode = (IntNode) config.get("version");
        if (versionNode != null && !versionNode.isNull()) {
            mappingsVersion = (Integer) versionNode.numberValue();
        }
        JsonNode node = null;
        if (mappingsVersion > 1) {
            String keySegment[] = parseOnfirst(name, ".");
            if (!keySegment[0].isEmpty() && !keySegment[1].isEmpty()) {
                node = config.get(keySegment[0]);
                if (node == null || node.isNull()) {
                    return null;        // 1st segment could not be located
                }
                node = node.get(keySegment[1]);
                if (node == null || node.isNull()) {
                    return null;        // 2nd segment could not be located
                }
            } else {
                return null;
            }
        } else {
            node = config.get(name);
            if (node == null || node.isNull()) {
                return null;        //specified name could not be located
            }
        }
        
        if (node.get("credentials") != null) {
            node = node.get("credentials");
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
                        case "user-provided":
                            value = getUserProvidedValue(token[1]);
                            break;
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
            try {
                value = JsonPath.parse(json).read(jsonPath);
            }
            catch (PathNotFoundException e) {
            }
        }
        return value;
    }

    // Search pattern resolvers


    private String getUserProvidedValue(String pattern) {
        LOGGER.debug("user-provided entry found:  " + pattern);
        String value = null;
        String vcap_services = getEnvironmentValue(VCAP_SERVICES);
        if (vcap_services == null || vcap_services.isEmpty() || pattern == null) {
            LOGGER.debug("No VCAP_SERVICES or no user-provided pattern");
            return null;
        }
        int i = pattern.lastIndexOf(":");
        if (i == -1 || i == pattern.length() - 1) {
            LOGGER.info("Invalid user-provided pattern");
            return null;
        }
        String serviceName = pattern.substring(0, i);
        String credentialKey = pattern.substring(i+1);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode vs = mapper.readTree(vcap_services);
            JsonNode userProvided = (ArrayNode) vs.get("user-provided");
            if (userProvided.isArray()) {
                ArrayNode array = (ArrayNode) userProvided;
                LOGGER.debug("Found user-provided array");
                for (final JsonNode entryNode : array) {
                    JsonNode nameNode = entryNode.get("name");
                    LOGGER.debug("Found user-provided array entry name field");
                    if (nameNode != null) {
                        LOGGER.debug("user-provided array entry name: " + nameNode.asText());
                        String name = nameNode.asText();
                        if (name != null && name.equals(serviceName)) {
                            JsonNode creds = entryNode.get("credentials");
                            if (creds != null) {
                                LOGGER.debug("Found user-provided array entry credentials");
                                value = JsonPath.parse(creds.toString()).read(credentialKey);
                                break;
                            }
                        }
                    }
                }
            }
            else {
                LOGGER.info("VCAP_SERVICES user-provided field is not an array");
            }
        } catch (Exception e) {
            LOGGER.info("Unexpected exception reading VCAP_SERVICES: " + e);
        }
        return value;
    }

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
            String path;
            if (mappingsVersion > 1) {
                path = target.startsWith("/") ? "file:" + target.trim() : "classpath:" + target.trim();
            }
            else {
                path = target.startsWith("/server/") ? "classpath:" + target.substring("/server/".length()) : "file:" + target.trim();
            }
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
            // Relative path or /server/ means it's a classpath resource
            if (!filePath.startsWith("/") || filePath.startsWith("/server/")) {
                String path = filePath.startsWith("/server/") ? filePath.substring("/server/".length()) : filePath;
                LOGGER.debug("Looking for classpath resource : " + path);
                JsonNode node = getJson(path);
                if (node != null) {
                    json = node.toString();
                    LOGGER.debug("Class path json : " + json);
                }
            } else {
                // absolute path
                LOGGER.debug("Looking for file: " + filePath);
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

    public void setAppContext(ConfigurableApplicationContext appContext) {
        this.appContext = appContext;
    }
}

