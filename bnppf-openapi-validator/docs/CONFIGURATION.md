# Configuration Guide

This guide covers all configuration options for the BNPPF OpenAPI Validator.

## Validation Levels

The validator supports three validation levels that control how strictly requests and responses are validated against the OpenAPI specification.

### LIGHT

Light validation is the most permissive mode, suitable for development and testing.

**What it validates:**
- Basic path and method matching
- Required fields (as warnings, not errors)
- Basic type validation (as warnings)

**What it ignores:**
- Additional properties in objects
- String format validation (email, uri, date-time, etc.)
- Pattern validation (regex)
- Minimum/maximum constraints
- Enum value validation (as warnings)

**When to use:**
- Development environments
- Initial API integration testing
- When you want to allow flexibility in request/response formats

**Configuration:**
```java
validator.setValidationLevel(ValidationLevel.LIGHT);
```

**CLI:**
```bash
openapi-validator validate-request --spec api.yaml --level light ...
```

**Axway:**
```groovy
msg.put("validationLevel", "light")
```

### LENIENT (Default)

Lenient validation provides a balance between strict compliance and practical flexibility.

**What it validates:**
- Path and method matching
- Required fields
- Data types
- Most constraints (min, max, minLength, maxLength)

**What it allows/ignores:**
- Additional properties in objects (allowed)
- Format validation (warnings only)
- Pattern validation (warnings only)

**When to use:**
- Staging environments
- General API validation
- When backend services may include extra fields

**Configuration:**
```java
validator.setValidationLevel(ValidationLevel.LENIENT);
```

### STRICT

Strict validation enforces full OpenAPI specification compliance.

**What it validates:**
- Everything in LENIENT, plus:
- Additional properties (rejected unless explicitly allowed in spec)
- String format validation (email, uri, date-time, uuid, etc.)
- Pattern validation (regex)
- Enum values
- All numeric constraints

**When to use:**
- Production environments
- Contract-first API development
- When full specification compliance is required

**Configuration:**
```java
validator.setValidationLevel(ValidationLevel.STRICT);
```

## Configuration Options

### Debug Mode

Enable detailed logging for troubleshooting.

**Java:**
```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(spec);
validator.setDebug(true);
```

**CLI:**
```bash
openapi-validator --debug validate-spec --spec api.yaml
```

**Axway:**
```groovy
msg.put("validationDebug", "true")
```

When enabled, debug mode logs:
- Incoming request/response details
- Path matching information
- Validation rule application
- Detailed error messages

### Query Parameter Decoding

By default, query parameters are URL-decoded before validation. This can be disabled if needed.

**Java:**
```java
validator.setDecodeQueryParams(false);
```

### Path Cache Size

The validator caches path mappings between exposed paths and specification paths. The default cache size is 1000 entries.

**Java:**
```java
validator.getExposurePath2SpecifiedPathMap().setMaxSize(5000);
```

**Axway:**
```groovy
validator.getExposurePath2SpecifiedPathMap().setMaxSize(10000)
```

For high-traffic APIs with many unique paths, increase this value to improve performance.

## Loading Specifications

### From Inline String

```java
String specContent = "openapi: '3.0.3'\ninfo:\n  title: My API...";
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(specContent);
```

### From File

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstanceFromFile("/path/to/openapi.yaml");
```

### From URL

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstanceFromUrl("https://api.example.com/openapi.yaml");
```

### With Validation Level

All factory methods accept an optional `ValidationLevel` parameter:

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(
    specContent,
    ValidationLevel.STRICT
);
```

## Singleton Behavior

The validator uses a singleton pattern per specification. Multiple calls with the same specification content will return the same validator instance.

```java
// These return the same instance (same spec content = same hash)
BnppfOpenAPIValidator v1 = BnppfOpenAPIValidator.getInstance(spec);
BnppfOpenAPIValidator v2 = BnppfOpenAPIValidator.getInstance(spec);
assert v1 == v2; // true
```

### Managing Instances

```java
// Get number of cached instances
int count = BnppfOpenAPIValidator.getInstanceCount();

// Clear all cached instances
BnppfOpenAPIValidator.clearAllInstances();

// Remove a specific instance
String hash = validator.getSpecHash();
BnppfOpenAPIValidator.removeInstance(hash);
```

## Environment-Specific Configurations

### Development

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(spec, ValidationLevel.LIGHT);
validator.setDebug(true);
```

### Staging

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(spec, ValidationLevel.LENIENT);
validator.setDebug(false);
```

### Production

```java
BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(spec, ValidationLevel.STRICT);
validator.setDebug(false);
validator.getExposurePath2SpecifiedPathMap().setMaxSize(10000);
```

## CLI Configuration

### Output Formats

**Text (default)** - Human-readable colored output:
```bash
openapi-validator validate-spec --spec api.yaml --output text
```

**JSON** - Machine-readable output:
```bash
openapi-validator validate-spec --spec api.yaml --output json
```

**JUnit** - CI/CD integration:
```bash
openapi-validator validate-spec --spec api.yaml --output junit > results.xml
```

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success - no errors |
| 1 | Validation errors found |
| 2 | Warnings found (with `--fail-on-warn`) |

### Fail on Warnings

```bash
# Exit with code 2 if warnings are found
openapi-validator validate-spec --spec api.yaml --fail-on-warn
```

### Disable Colors

```bash
openapi-validator validate-spec --spec api.yaml --no-color
```

## Axway Configuration

### Setting Attributes

In Policy Studio, use "Set Message Attribute" filters before the validation script:

| Attribute | Required | Default | Description |
|-----------|----------|---------|-------------|
| `specFile` | Yes | - | Path to OpenAPI spec file |
| `validationLevel` | No | `lenient` | light, lenient, or strict |
| `validationDebug` | No | `false` | Enable debug logging |

### Using Environment Variables

```
specFile = ${env.API_SPEC_PATH}
validationLevel = ${env.VALIDATION_LEVEL}
```

### Dynamic Validation Level

Use a selector to set validation level based on environment:

```
validationLevel = ${environment.type == 'production' ? 'strict' : 'lenient'}
```

## Logging Configuration

### Logback (CLI)

The CLI module includes a `logback.xml` configuration. To customize:

1. Create a custom `logback.xml`:
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="be.bnppf.openvalidator" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

2. Pass it to the JVM:
```bash
java -Dlogback.configurationFile=/path/to/logback.xml -jar validator.jar ...
```

### SLF4J in Axway

Axway uses its own logging framework. The validator logs via SLF4J, which can be bridged to Axway's trace facility.

## Performance Tuning

### Cache Size

For APIs with many unique paths (e.g., paths with UUIDs), increase the cache:

```java
validator.getExposurePath2SpecifiedPathMap().setMaxSize(50000);
```

### Validation Level

Use `LIGHT` or `LENIENT` for better performance when strict validation isn't required.

### Instance Reuse

The singleton pattern ensures specification parsing happens only once. Avoid calling `clearAllInstances()` in production unless necessary.

### JVM Options

For high-throughput scenarios:

```bash
java -Xms512m -Xmx1g \
     -XX:+UseG1GC \
     -jar bnppf-openapi-validator-cli.jar ...
```

## Next Steps

- [Axway Integration Guide](AXWAY-INTEGRATION.md) - Set up Policy Studio
- [Installation Guide](INSTALLATION.md) - Install the validator
