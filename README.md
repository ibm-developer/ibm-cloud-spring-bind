# IBM Cloud Service Bindings For Spring
### *** <i>This project is currently under development and is not yet published to Maven Central ***</i>

This library provides Spring Boot auto configuration for accessing IBM Cloud services config data.

* [Overview](#overview)
* [Usage](#usage)
* [Binding IBM Cloud Services to a Spring Boot Application](#Binding-IBM-Cloud-Services-to-a-Spring-Boot Application)
* [Related Documentation](#related-documentation)
* [Development](#development)  
  * [Contributing](#contributing)
  * [Testing](#testing)
  * [Using in Other Projects](#using-in-other-projects)
  * [License](#license)
  * [Issues](#issues)

## Overview

This library allows easy access to IBM Cloud services configuration data for services that have been bound to a Spring Boot application.

Including the dependency `ibm-cloud-spring-service-bind` auto configures a <code>PropertySource</code> to allow service configuration using Spring property source mechanisms such as <code>@Value</code> annotations, <code>Environment</code> objects, etc.

Multiple runtime environments are supported such as:  

- IBM Cloud - Kubernetes
- IBM Cloud - Cloud Foundry
- local / on prem - access to IBM Cloud hosted services

The supported methods of binding a service to your application are:  
   
* IBM Cloud App Services console
* IBM Cloud Developer Tools CLI
* Manual Binding

## Usage

Dependency configuration:

**Maven:**

```xml
<dependency>
   <groupId>com.ibm.cloud</groupId>
   <artifactId>ibm-cloud-spring-service-bind</artifactId>
   <version>0.0.1</version>
</dependency>
```

**Gradle:**

```groovy
dependencies {
    compile group: 'com.ibm.cloud', name: 'ibm-cloud-spring-service-bind', version: '0.0.1'
}
```

To access service configuration in your code you can use the <code>@Value</code> annotation, or use the Spring framework `Environment` class' <code>getProperty()</code> method. 

**Example:** Accessing configuration data for an IBM Cloudant NoSQL DB service:

~~~ java
     @Autowired
     Environment env; 

     @Value("cloudant.url")
     String cloudantUrl;

     String cloudantUsername = 
          env.getProperty("cloudant.username");
~~~

## Binding IBM Cloud Services to a Spring Boot Application

Your application must be bound to an IBM Cloud service in order to use this library. The following methods of service binding are supported:

1. IBM Cloud App Services Console  
You can use the [IBM Cloud App Services Console](https://console.bluemix.net/developer/appservice/dashboard) to create Spring Boot apps, create services, and bind the services to apps.
2. IBM Cloud Developer Tools CLI  
You can also use the [IBM Cloud Developer Tools CLI](https://console.bluemix.net/docs/cloudnative/idt/index.html#developercli) to create a Spring Boot app, create services, and bind the services to the app.
3. Manually create an IBM Cloud service and add the configuration values to the `application.properties` file.  
<p>For example, from the [IBM Cloud Services Catalg](https://console.bluemix.net/catalog/) select the [Cloudant NoSQL DB](https://console.bluemix.net/catalog/services/cloudant-nosql-db) service and click Create to create a  Cloudant service.</p>  
<p>On the Service Credentials tab of the service select the View Credentials drop down and copy the `username`, `password`, and `url` values to your app's `application.properties` file as follows:  

    ~~~
    cloudant.username=62c520dc-9367...  
    cloudant.password=8c03bd171cd99...
    cloudant.url=https://62c520dc-9367...cloudant.com
    ~~~  
 


## Related documentation
* [IBM Cloud Developer Tools CLI](https://console.bluemix.net/docs/cloudnative/idt/index.html#developercli)
* [IBM Cloud App Services Console](https://console.bluemix.net/developer/appservice/dashboard)
* [IBM Cloud Services Catalg](https://console.bluemix.net/catalog/)
* [Spring Boot documentation](https://projects.spring.io/spring-boot/)

# Development

### Contributing

For information about contributing see...<i>more to come here</i>.

### Testing

At this time you can only test locally as the dependency is not yet in Maven Central. 

To test locally first build the project by issuing `mvn package`. Make sure the unit tests all pass.

After a successful build you can install the dependency locally with the following command:

`mvn install:install-file -Dfile=target/ibm-cloud-spring-service-bind-1.0.0.jar -DpomFile=pom.xml`

You can now include the dependency in another project and use as described above in [usage](#usage)


### Using in Other Projects

The preferred approach for using ibm-cloud-spring-service-bind in other projects is to use the Gradle or Maven dependency as described above.

### License

Copyright © 2018 IBM Corp. All rights reserved.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

### Issues

Before opening a new issue please consider the following:

* Please try to reproduce the issue using the latest version.
* Please check the [existing issues](https://github.com/ibm-developer/ibm-cloud-spring-bind/issues)
to see if the problem has already been reported. Note that the default search
includes only open issues, but it may already have been closed.
* When opening a new issue [here in github](../../issues) please complete the template fully.
