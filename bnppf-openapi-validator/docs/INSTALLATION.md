# Installation Guide

This guide explains how to install and set up the BNPPF OpenAPI Validator.

## Prerequisites

- **Java 11** or higher
- **Apache Maven 3.6** or higher (for building from source)
- **Axway API Gateway 7.7** or higher (for gateway integration)

## Building from Source

### Clone the Repository

```bash
git clone <repository-url>
cd bnppf-openapi-validator
```

### Build All Modules

```bash
mvn clean package
```

This will build:
- `bnppf-openapi-validator-core-1.0.0-SNAPSHOT.jar` - Core validation library
- `bnppf-openapi-validator-cli-1.0.0-SNAPSHOT.jar` - CLI tool (uber-JAR with all dependencies)

### Run Tests

```bash
mvn test
```

### Generate Test Coverage Report

```bash
mvn test jacoco:report
```

Coverage reports will be available at:
- `bnppf-openapi-validator-core/target/site/jacoco/index.html`
- `bnppf-openapi-validator-cli/target/site/jacoco/index.html`

## Installing the CLI Tool

### Option 1: Use the Uber-JAR

The CLI module builds an executable uber-JAR with all dependencies included:

```bash
# Copy to a convenient location
cp bnppf-openapi-validator-cli/target/bnppf-openapi-validator-cli-1.0.0-SNAPSHOT.jar /usr/local/lib/

# Create a shell script wrapper
cat > /usr/local/bin/openapi-validator << 'EOF'
#!/bin/bash
java -jar /usr/local/lib/bnppf-openapi-validator-cli-1.0.0-SNAPSHOT.jar "$@"
EOF

chmod +x /usr/local/bin/openapi-validator
```

### Option 2: Add to PATH

Add an alias to your shell configuration:

```bash
# Add to ~/.bashrc or ~/.zshrc
alias openapi-validator='java -jar /path/to/bnppf-openapi-validator-cli-1.0.0-SNAPSHOT.jar'
```

### Verify Installation

```bash
openapi-validator --version
# Output: 1.0.0

openapi-validator --help
```

## Installing in Axway API Gateway

### Step 1: Copy JAR Files

Copy the core library and its dependencies to the Axway Gateway's `ext/lib` directory:

```bash
AXWAY_HOME=/opt/Axway/apigateway

# Core library
cp bnppf-openapi-validator-core/target/bnppf-openapi-validator-core-1.0.0-SNAPSHOT.jar \
   ${AXWAY_HOME}/ext/lib/

# Copy dependencies
mvn dependency:copy-dependencies -f bnppf-openapi-validator-core/pom.xml
cp bnppf-openapi-validator-core/target/dependency/*.jar ${AXWAY_HOME}/ext/lib/
```

**Required dependencies:**
- swagger-request-validator-core-2.40.0.jar
- swagger-parser-2.1.22.jar
- swagger-parser-v3-2.1.22.jar
- swagger-parser-v2-converter-2.1.22.jar
- swagger-core-2.2.x.jar
- swagger-models-2.2.x.jar
- jackson-databind-2.17.0.jar
- jackson-core-2.17.0.jar
- jackson-annotations-2.17.0.jar
- jackson-dataformat-yaml-2.17.0.jar
- jackson-datatype-jsr310-2.17.0.jar
- snakeyaml-2.x.jar
- json-schema-validator-1.x.jar
- slf4j-api-2.0.x.jar
- And other transitive dependencies

### Step 2: Deploy Groovy Scripts

Copy the Groovy scripts to a location accessible by Policy Studio:

```bash
cp scripts/groovy/*.groovy ${AXWAY_HOME}/scripts/bnppf/
```

Or deploy them directly into your Policy Studio project.

### Step 3: Deploy OpenAPI Specifications

Create a directory for your API specifications:

```bash
mkdir -p ${AXWAY_HOME}/specs
cp /path/to/your/openapi.yaml ${AXWAY_HOME}/specs/
```

### Step 4: Restart Axway Gateway

```bash
# Using Node Manager
${AXWAY_HOME}/posix/bin/nodemanager -k
${AXWAY_HOME}/posix/bin/nodemanager

# Or restart via Admin Node Manager UI
```

### Step 5: Configure Policy Studio

See [AXWAY-INTEGRATION.md](AXWAY-INTEGRATION.md) for detailed Policy Studio configuration.

## Installing as Maven Dependency

If you want to use the validator in another Maven project:

### Install to Local Repository

```bash
mvn install
```

### Add Dependency

```xml
<dependency>
    <groupId>be.bnppf</groupId>
    <artifactId>bnppf-openapi-validator-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Docker Installation (Optional)

### Build Docker Image

```dockerfile
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/bnppf-openapi-validator-cli/target/bnppf-openapi-validator-cli-1.0.0-SNAPSHOT.jar /app/validator.jar
ENTRYPOINT ["java", "-jar", "/app/validator.jar"]
```

### Build and Run

```bash
docker build -t bnppf-openapi-validator .
docker run --rm -v /path/to/specs:/specs bnppf-openapi-validator \
    validate-spec --spec /specs/openapi.yaml
```

## Troubleshooting

### Java Version Issues

Ensure Java 11 or higher is installed:

```bash
java -version
# Should show Java 11 or higher
```

### Maven Build Failures

If the build fails due to dependency issues:

```bash
# Clear local Maven cache
rm -rf ~/.m2/repository/be/bnppf

# Rebuild with debug output
mvn clean package -X
```

### Axway ClassLoader Issues

If classes are not found in Axway:

1. Verify JARs are in `ext/lib` and readable
2. Check for duplicate JAR versions
3. Review Axway trace logs for class loading errors
4. Restart all Axway processes

### Permission Issues

Ensure proper file permissions:

```bash
chmod 644 ${AXWAY_HOME}/ext/lib/*.jar
chmod 755 ${AXWAY_HOME}/scripts/bnppf/
chmod 644 ${AXWAY_HOME}/scripts/bnppf/*.groovy
```

## Next Steps

- [Configuration Guide](CONFIGURATION.md) - Configure validation options
- [Axway Integration Guide](AXWAY-INTEGRATION.md) - Set up Policy Studio
