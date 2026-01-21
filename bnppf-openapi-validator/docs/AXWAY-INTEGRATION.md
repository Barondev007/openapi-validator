# Axway API Gateway Integration Guide

This guide explains how to integrate the BNPPF OpenAPI Validator with Axway API Gateway using Policy Studio.

## Overview

The integration consists of:
1. **Core Library JARs** - Deployed to Axway's `ext/lib` directory
2. **Groovy Scripts** - Request and response validation scripts
3. **Policy Studio Configuration** - Scripting filters and attribute setup

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Axway API Gateway                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐   ┌──────────────────┐   ┌─────────────────────┐  │
│  │  Incoming   │──▶│  Request         │──▶│  Route to Backend   │  │
│  │  Request    │   │  Validation      │   │                     │  │
│  └─────────────┘   │  (Groovy Script) │   └──────────┬──────────┘  │
│                    └──────────────────┘              │             │
│                                                      ▼             │
│  ┌─────────────┐   ┌──────────────────┐   ┌─────────────────────┐  │
│  │  Response   │◀──│  Response        │◀──│  Backend Response   │  │
│  │  to Client  │   │  Validation      │   │                     │  │
│  └─────────────┘   │  (Groovy Script) │   └─────────────────────┘  │
│                    └──────────────────┘                            │
│                            │                                       │
│                            ▼                                       │
│                    ┌──────────────────┐                           │
│                    │  OpenAPI Spec    │                           │
│                    │  (/specs/*.yaml) │                           │
│                    └──────────────────┘                           │
└─────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

1. **Axway API Gateway 7.7+** installed and configured
2. **Policy Studio** connected to your gateway
3. **BNPPF OpenAPI Validator** JARs deployed (see [INSTALLATION.md](INSTALLATION.md))

## Step-by-Step Setup

### Step 1: Deploy Library JARs

Copy all required JARs to `${VDISTDIR}/ext/lib/`:

```bash
# Core library
cp bnppf-openapi-validator-core-1.0.0-SNAPSHOT.jar ${VDISTDIR}/ext/lib/

# Dependencies (get from Maven)
mvn dependency:copy-dependencies -f bnppf-openapi-validator-core/pom.xml
cp target/dependency/*.jar ${VDISTDIR}/ext/lib/
```

**Note:** After copying JARs, restart the gateway:
```bash
${VDISTDIR}/posix/bin/nodemanager -k
${VDISTDIR}/posix/bin/nodemanager
```

### Step 2: Deploy OpenAPI Specifications

Create a directory for API specifications:

```bash
mkdir -p ${VDISTDIR}/specs
```

Copy your OpenAPI specifications:
```bash
cp my-api-openapi.yaml ${VDISTDIR}/specs/
```

### Step 3: Import Groovy Scripts

#### Option A: Project Scripts

1. In Policy Studio, right-click your project
2. Select **Import** > **File System**
3. Import the Groovy scripts from `scripts/groovy/`

#### Option B: External Scripts

1. Copy scripts to a shared location:
   ```bash
   cp scripts/groovy/*.groovy ${VDISTDIR}/scripts/bnppf/
   ```
2. Reference the external path in your Scripting Filters

### Step 4: Create Request Validation Filter

1. In Policy Studio, open your API policy
2. Right-click the policy canvas
3. Select **Scripting Language** > **Add Scripting Filter**
4. Configure the filter:

   **General Tab:**
   - **Name:** `Validate Request`
   - **Script Language:** `Groovy`
   - **Script:** Click **Browse** and select `BnppfRequestValidation.groovy`

   **Or for external script:**
   - **Script Location:** `file:///opt/Axway/apigateway/scripts/bnppf/BnppfRequestValidation.groovy`

### Step 5: Create Response Validation Filter

1. Add another Scripting Filter after your routing filter
2. Configure:

   **General Tab:**
   - **Name:** `Validate Response`
   - **Script Language:** `Groovy`
   - **Script:** Select `BnppfResponseValidation.groovy`

### Step 6: Set Message Attributes

Before the validation filters, add a **Set Message Attribute** filter:

1. Right-click > **Attribute Filters** > **Set Message Attribute**
2. Add the following attributes:

| Attribute Name | Attribute Value | Type |
|----------------|-----------------|------|
| `specFile` | `/opt/Axway/apigateway/specs/my-api-openapi.yaml` | String |
| `validationLevel` | `lenient` | String |
| `validationDebug` | `false` | String |

**Using Environment Variables:**

| Attribute Name | Attribute Value |
|----------------|-----------------|
| `specFile` | `${env.API_SPEC_PATH}` |
| `validationLevel` | `${env.VALIDATION_LEVEL:lenient}` |
| `validationDebug` | `${env.VALIDATION_DEBUG:false}` |

### Step 7: Create Error Response Filter

Create a **Set Message** filter for validation error responses:

1. Right-click > **Conversion Filters** > **Set Message**
2. Configure:

   **Content Tab:**
   ```json
   {
     "error": {
       "code": 400,
       "message": "Request validation failed",
       "details": "${validation.errors}"
     }
   }
   ```

   **Content-Type:** `application/json`

### Step 8: Wire the Policy

Connect the filters in this order:

```
┌───────────────────┐
│ Set Attributes    │
│ (specFile, etc.)  │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐     ┌──────────────────┐
│ Validate Request  │────▶│ Set Error Message│────▶ Return 400
│ (Groovy Script)   │FAIL │                  │
└────────┬──────────┘     └──────────────────┘
         │PASS
         ▼
┌───────────────────┐
│ Route to Backend  │
│                   │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐     ┌──────────────────┐
│ Validate Response │────▶│ Log Warning      │
│ (Groovy Script)   │FAIL │ (Continue)       │
└────────┬──────────┘     └──────────────────┘
         │PASS
         ▼
┌───────────────────┐
│ Return Response   │
│                   │
└───────────────────┘
```

## Complete Policy Example

### Example: Petstore API Validation

```
Policy: Petstore API

1. [Set Message Attribute]
   - specFile = /opt/Axway/specs/petstore-openapi.yaml
   - validationLevel = strict
   - validationDebug = false

2. [Scripting Filter: Validate Request]
   - Script: BnppfRequestValidation.groovy
   - On Success: Continue
   - On Failure: Go to "Validation Error Response"

3. [Connect to URL]
   - URL: ${http.destination.url}
   - On Success: Continue
   - On Failure: Go to "Backend Error Handler"

4. [Scripting Filter: Validate Response]
   - Script: BnppfResponseValidation.groovy
   - On Success: Continue
   - On Failure: Continue (log only)

5. [Reflect Message]
   - Return response to client

---

6. [Validation Error Response] (Failure path from step 2)
   - [Set Message]
     Content: {"error": {"code": 400, "message": "${circuit.failure.reason}"}}
     Content-Type: application/json
   - [Reflect Message]
     Status: ${http.response.status}
```

## Dynamic Specification Selection

For APIs that serve multiple specifications, use selectors:

### Based on Path Prefix

```
specFile = ${http.request.path.startsWith('/v1/') ?
            '/specs/api-v1.yaml' : '/specs/api-v2.yaml'}
```

### Based on Header

```
specFile = /specs/${http.header.X-API-Version}.yaml
```

### Based on Virtual Host

```
specFile = /specs/${api.name}-openapi.yaml
```

## Conditional Validation

### Skip Validation for Health Checks

Add a condition before the validation filter:

```
[Compare Attribute]
- Attribute: ${http.request.path}
- Operator: does not equal
- Value: /health

On Match: Continue to validation
On No Match: Skip to routing
```

### Validate Only in Production

```
[Compare Attribute]
- Attribute: ${environment.name}
- Operator: equals
- Value: production

On Match: Continue to validation
On No Match: Skip validation
```

## Handling Validation Results

### Accessing Validation Data

After validation, these attributes are available:

| Attribute | Description |
|-----------|-------------|
| `validation.result` | Boolean - true if valid |
| `validation.errors` | String - error messages |
| `validation.report.json` | String - full report as JSON |

### Custom Error Responses

Use the attributes in your error response:

```json
{
  "timestamp": "${system.time}",
  "status": ${http.response.status},
  "error": "Bad Request",
  "message": "${validation.errors}",
  "path": "${http.request.path}"
}
```

### Logging Validation Failures

Add a **Trace** filter on the failure path:

```
Level: ERROR
Message: Validation failed for ${http.request.verb} ${http.request.path}: ${validation.errors}
```

## Performance Considerations

### Caching

The validator caches:
- Parsed OpenAPI specifications (by content hash)
- Path mappings (configurable size)

For high-traffic APIs, increase the path cache:

```groovy
// In your Groovy script, before validation
validator.getExposurePath2SpecifiedPathMap().setMaxSize(10000)
```

### Validation Level

Use appropriate validation levels:
- `light` - Development/testing
- `lenient` - Most use cases (default)
- `strict` - Production/compliance

### Async Validation

For response validation that shouldn't block:
1. Configure the validation filter to continue on failure
2. Use alert policies for validation failures

## Troubleshooting

### Common Issues

#### 1. ClassNotFoundException

**Symptom:** `java.lang.ClassNotFoundException: be.bnppf.openvalidator.BnppfOpenAPIValidator`

**Solution:**
1. Verify JAR is in `ext/lib`
2. Restart gateway
3. Check file permissions

#### 2. Specification Not Found

**Symptom:** `Failed to read specification file: /path/to/spec.yaml`

**Solution:**
1. Verify file path is correct
2. Check file permissions
3. Use absolute paths

#### 3. Validation Always Fails

**Symptom:** All requests fail validation

**Solution:**
1. Enable debug mode: `msg.put("validationDebug", "true")`
2. Check the Axway trace logs
3. Verify the specification path matches your API paths
4. Test with CLI first

#### 4. Performance Issues

**Symptom:** High latency on first requests

**Solution:**
1. Pre-warm the validator on gateway startup
2. Increase heap size for Axway
3. Use `LENIENT` validation level

### Enabling Traces

In Axway Node Manager:
1. Go to **Settings** > **Logging**
2. Set trace level to **DEBUG** for troubleshooting
3. Filter logs by `BnppfRequestValidation` or `BnppfResponseValidation`

### Testing Outside Axway

Use the CLI to test your specification:

```bash
# Test the spec itself
openapi-validator validate-spec --spec /path/to/spec.yaml --level strict

# Test a request
openapi-validator validate-request \
    --spec /path/to/spec.yaml \
    --method POST \
    --path /pets \
    --body '{"name": "Fluffy"}' \
    --content-type application/json \
    --level strict
```

## Security Considerations

1. **Specification Access:** Store specs in a secure location
2. **Error Messages:** Sanitize error messages in production to avoid leaking schema details
3. **Logging:** Be careful not to log sensitive request/response data
4. **Validation Level:** Use `STRICT` in production for security-sensitive APIs

## Best Practices

1. **Version Control:** Keep specs in Git alongside API code
2. **CI/CD:** Validate specs in your pipeline before deployment
3. **Monitoring:** Alert on validation failure rates
4. **Documentation:** Keep specs and policies in sync
5. **Testing:** Test validation with both valid and invalid requests

## Next Steps

- [Configuration Guide](CONFIGURATION.md) - Detailed configuration options
- [Installation Guide](INSTALLATION.md) - Complete installation instructions
