# Spring Cloud Contract for Mountebank

[![Apache 2.0](https://img.shields.io/github/license/micrometer-metrics/micrometer.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Spring Cloud Contract Mountebank is extension plug-in for Spring Cloud Contract https://spring.io/projects/spring-cloud-contract). The implementation base on Pluggable Architecture guide (https://docs.spring.io/spring-cloud-contract/docs/2.2.4.RELEASE/reference/html/advanced.html#customization-pluggable-architecture) and some example code from (https://github.com/spring-cloud/spring-cloud-contract/tree/master/spring-cloud-contract-tools)

Mountebank (http://www.mbtest.org/) is lightweight stub framework. Stub is created from predicates .ejs file, so this plug-in help to convert DSL groovy contract to Mountebank's predicates file. You can use output predicates files to create Mountebank stub instantly.

by default, Mountebank's predicates files were generated in ***target/stubs/META-INF/{groupId}/{artifactId}/{version}***



## Pre-Requisite

Your application is implemented Spring Cloud Contract as producer side. You can use either Gradle or Maven plugins. Below you can find an example for Maven.

## Add library to Spring Cloud Contract Maven Plugin dependency

```xml
<plugin>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-contract-maven-plugin</artifactId>
    <version>${spring-cloud-contract.version}</version>
    <extensions>true</extensions>
    <configuration>
        <packageWithBaseClasses>your.package.base.class</packageWithBaseClasses>
    </configuration>
    <!-- add the extension plug-in  -->
    <dependencies>
        <dependency>
            <groupId>com.ascendcorp</groupId>
            <artifactId>spring-cloud-contract-mountebank</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## Command  

```java
mvn spring-cloud-contract:convert
```

## Setup stub 
http://www.mbtest.org/docs/gettingStarted



Feel free to contribute and make it better!

-------------------------------------
_Licensed under [Apache Software License 2.0](https://www.apache.org/licenses/LICENSE-2.0)_
