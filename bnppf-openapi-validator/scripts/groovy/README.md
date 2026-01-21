# BNPPF OpenAPI Validator - Groovy Scripts for Axway API Gateway

This directory contains Groovy scripts for integrating the BNPPF OpenAPI Validator with Axway API Gateway.

## Scripts

### BnppfRequestValidation.groovy

Validates incoming HTTP requests against an OpenAPI specification.

**Usage:**
- Place in a Scripting Filter before your main policy logic
- Fails the circuit (returns false) if validation fails
- Sets `http.response.status` to 400 for validation failures

### BnppfResponseValidation.groovy

Validates outgoing HTTP responses against an OpenAPI specification.

**Usage:**
- Place in a Scripting Filter after the backend service call
- Logs validation errors but can be configured to pass through invalid responses
- Does NOT modify `http.response.status` to preserve backend response

## Prerequisites

1. **Java 11 or higher** on the Axway Gateway
2. **BNPPF OpenAPI Validator Core JAR** deployed to `${VDISTDIR}/ext/lib`
3. All dependency JARs deployed to `${VDISTDIR}/ext/lib`

## Installation

### Step 1: Build the Validator

```bash
cd bnppf-openapi-validator
mvn clean package
```

### Step 2: Deploy JARs

Copy the following JARs to your Axway Gateway's `ext/lib` directory:

```bash
# Core validator JAR
cp bnppf-openapi-validator-core/target/bnppf-openapi-validator-core-1.0.0-SNAPSHOT.jar ${VDISTDIR}/ext/lib/

# Required dependencies (check target/dependency/ after running mvn dependency:copy-dependencies)
# - swagger-request-validator-core-2.40.0.jar
# - swagger-parser-2.1.22.jar
# - jackson-databind-2.17.0.jar
# - jackson-dataformat-yaml-2.17.0.jar
# - And their transitive dependencies
```

Alternatively, use the `maven-assembly-plugin` to create an uber-JAR with all dependencies.

### Step 3: Deploy Groovy Scripts

Copy the Groovy scripts to your Policy Studio project or a shared scripts directory:

```bash
cp scripts/groovy/*.groovy ${POLICY_PROJECT}/scripts/
```

### Step 4: Restart Axway Gateway

After deploying the JARs, restart the Axway Gateway to pick up the new libraries:

```bash
# Linux
${VDISTDIR}/posix/bin/nodemanager -k
${VDISTDIR}/posix/bin/nodemanager

# Or restart via Admin Node Manager
```

## Configuration in Policy Studio

### Creating a Request Validation Filter

1. Open Policy Studio and your project
2. Create a new **Scripting Filter**
3. Configure the filter:
   - **Name:** Request Validation
   - **Language:** Groovy
   - **Script:** Select or import `BnppfRequestValidation.groovy`

4. Add the filter to your policy **before** routing to the backend

### Creating a Response Validation Filter

1. Create another **Scripting Filter**
2. Configure:
   - **Name:** Response Validation
   - **Language:** Groovy
   - **Script:** Select or import `BnppfResponseValidation.groovy`

3. Add the filter to your policy **after** the backend response

### Setting Message Attributes

Before the validation filters, use a **Set Message Attribute** filter to configure:

| Attribute | Value | Description |
|-----------|-------|-------------|
| `specFile` | `/path/to/openapi.yaml` | **Required.** Path to the OpenAPI spec file |
| `validationLevel` | `lenient` | Optional. One of: `light`, `lenient`, `strict` |
| `validationDebug` | `false` | Optional. Enable debug logging |

You can also use environment variables or Axway selectors:

```
specFile = ${env.API_SPEC_PATH}
validationLevel = ${env.VALIDATION_LEVEL}
validationDebug = ${env.VALIDATION_DEBUG}
```

## Validation Levels

| Level | Description |
|-------|-------------|
| `light` | Minimal validation. Ignores additional properties, formats, and patterns. Good for development. |
| `lenient` | Default. Validates types and required fields, allows additional properties. |
| `strict` | Full OpenAPI compliance. Validates everything including formats, patterns, and enums. |

## Message Attributes Set by Scripts

### Request Validation

| Attribute | Type | Description |
|-----------|------|-------------|
| `validation.result` | Boolean | `true` if valid, `false` otherwise |
| `validation.errors` | String | Error messages (empty if valid) |
| `validation.report.json` | String | Full validation report as JSON |
| `http.response.status` | Integer | Set to 400 if validation fails |
| `circuit.failure.reason` | String | Error details for circuit failure |

### Response Validation

| Attribute | Type | Description |
|-----------|------|-------------|
| `validation.result` | Boolean | `true` if valid, `false` otherwise |
| `validation.errors` | String | Error messages (empty if valid) |
| `validation.report.json` | String | Full validation report as JSON |

## Example Policy Flow

```
[Client Request]
       |
       v
[Set specFile attribute]
       |
       v
[BnppfRequestValidation] --[FAIL]--> [Return 400 Bad Request]
       |
       [PASS]
       |
       v
[Route to Backend]
       |
       v
[BnppfResponseValidation] --[FAIL]--> [Log Warning] --> [Continue]
       |
       [PASS]
       |
       v
[Return Response to Client]
```

## Error Responses

When request validation fails, the script sets `http.response.status` to 400. You can use a **Set Message** filter to return a custom error response:

```json
{
  "error": {
    "code": 400,
    "message": "Request validation failed",
    "details": "${validation.errors}"
  }
}
```

## Troubleshooting

### ClassNotFoundException

If you see `ClassNotFoundException: be.bnppf.openvalidator.BnppfOpenAPIValidator`:
1. Verify the JAR is in `${VDISTDIR}/ext/lib`
2. Restart the Axway Gateway
3. Check the Gateway logs for class loading errors

### NoClassDefFoundError

If you see `NoClassDefFoundError` for dependency classes:
1. Ensure all required dependency JARs are deployed
2. Check for version conflicts with existing Axway libraries

### Validation Not Working

1. Enable debug mode: `msg.put("validationDebug", "true")`
2. Check the Axway trace files for validation logs
3. Verify the spec file path is correct and accessible
4. Test the spec file with the CLI first

### Performance Issues

1. The validator caches parsed specifications by hash
2. For high-traffic APIs, consider increasing the path cache size:
   ```groovy
   validator.getExposurePath2SpecifiedPathMap().setMaxSize(10000)
   ```
3. Use `LIGHT` or `LENIENT` validation for non-production environments

## Support

For issues and feature requests, contact the BNPPF API team.
