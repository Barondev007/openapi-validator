/**
 * Groovy script for Axway API Gateway request validation.
 *
 * This script validates incoming HTTP requests against an OpenAPI specification.
 * It should be used in a Scripting Filter within Axway Policy Studio.
 *
 * Required Axway message attributes:
 * - specFile: Path to the OpenAPI specification file (required)
 * - validationLevel: Validation strictness - light, lenient, or strict (optional, default: lenient)
 * - validationDebug: Enable debug logging - true/false (optional, default: false)
 *
 * Output:
 * - Sets 'validation.result' to true/false
 * - Sets 'validation.errors' with error messages if validation fails
 * - Sets 'http.response.status' to 400 if validation fails
 * - Sets 'circuit.failure.reason' with validation error details
 *
 * Example Policy Studio configuration:
 * 1. Create a Scripting Filter
 * 2. Set the scripting language to Groovy
 * 3. Set the script path to this file
 * 4. Add message attributes:
 *    - specFile: ${env.SPEC_FILE} or /path/to/openapi.yaml
 *    - validationLevel: ${validationLevel} or "lenient"
 *    - validationDebug: ${validationDebug} or "false"
 *
 * @author BNPPF
 * @version 1.0.0
 */

import be.bnppf.openvalidator.BnppfOpenAPIValidator
import be.bnppf.openvalidator.ValidationLevel
import be.bnppf.openvalidator.ValidationReport
import com.vordel.trace.Trace

/**
 * Main entry point called by Axway Gateway.
 *
 * @param msg The Axway message object containing request data and attributes
 * @return true if validation passes, false otherwise
 */
def invoke(msg) {
    try {
        // Get the specification file path from Axway variable
        def specFile = msg.get("specFile")
        if (specFile == null || specFile.toString().trim().isEmpty()) {
            Trace.error("specFile variable is not set")
            msg.put("circuit.failure.reason", "OpenAPI specification file not configured")
            msg.put("http.response.status", 500)
            return false
        }

        // Get validation level (optional, default: LENIENT)
        def levelStr = msg.get("validationLevel") ?: "lenient"
        def validationLevel = ValidationLevel.fromString(levelStr.toString())

        // Get debug setting (optional, default: false)
        def debugStr = msg.get("validationDebug") ?: "false"
        def debug = "true".equalsIgnoreCase(debugStr.toString())

        // Get or create validator instance
        def validator = BnppfOpenAPIValidator.getInstanceFromFile(specFile.toString())
        validator.setValidationLevel(validationLevel)
        validator.setDebug(debug)

        // Optionally configure cache size for high-traffic scenarios
        // validator.getExposurePath2SpecifiedPathMap().setMaxSize(5000)

        // Get request details from the message
        def payload = bodyAsString(msg.get('content.body'))
        def path = msg.get("http.request.path")?.toString() ?: ""
        def verb = msg.get("http.request.verb")?.toString()?.toUpperCase() ?: "GET"

        // Get query parameters
        def queryParams = msg.get("params.query")

        // Get headers
        def headers = msg.get("http.headers")
        def contentHeaders = msg.get("http.content.headers")

        if (debug) {
            Trace.info("BnppfRequestValidation: Validating request [path: ${path}, method: ${verb}, level: ${validationLevel}]")
        }

        // Merge Content-Type header from content headers if available
        if (contentHeaders != null && headers != null) {
            try {
                headers.addHeaders(contentHeaders)
            } catch (Exception e) {
                if (debug) {
                    Trace.info("BnppfRequestValidation: Could not merge content headers: ${e.message}")
                }
            }
        }

        // Convert Axway HeaderSet to Map if needed
        def headerMap = convertHeaders(headers)
        def queryMap = convertQueryParams(queryParams)

        // Perform validation with detailed report
        def validationReport = validator.validateRequest(payload, verb, path, queryMap, headerMap)

        if (validationReport.hasErrors()) {
            def errorMessages = validationReport.getMessagesAsString()

            if (debug) {
                Trace.info("BnppfRequestValidation: Validation failed: ${errorMessages}")
            }

            msg.put("validation.result", false)
            msg.put("validation.errors", errorMessages)
            msg.put("validation.report.json", validationReport.toJson())
            msg.put("circuit.failure.reason", errorMessages)
            msg.put("http.response.status", 400)

            return false
        }

        if (debug && validationReport.hasWarnings()) {
            Trace.info("BnppfRequestValidation: Validation warnings: ${validationReport.getWarnings()}")
        }

        msg.put("validation.result", true)
        msg.put("validation.errors", "")

        return true

    } catch (IllegalArgumentException e) {
        Trace.error("BnppfRequestValidation: Configuration error - ${e.message}")
        msg.put("circuit.failure.reason", "Validation configuration error: ${e.message}")
        msg.put("http.response.status", 500)
        return false
    } catch (Exception e) {
        Trace.error("BnppfRequestValidation: Error during request validation", e)
        msg.put("circuit.failure.reason", "Internal validation error: ${e.message}")
        msg.put("http.response.status", 500)
        return false
    }
}

/**
 * Convert the request body to a String.
 *
 * @param body The body object from the message
 * @return The body as a String, or null if not available
 */
def bodyAsString(body) {
    if (body == null) {
        return null
    }
    try {
        // Handle different body types in Axway
        if (body instanceof String) {
            return body
        }
        // Try to get input stream for body types that support it
        def inputStream = body.getInputStream(0)
        if (inputStream != null) {
            return inputStream.text
        }
        return body.toString()
    } catch (IOException e) {
        Trace.error("BnppfRequestValidation: Error converting body to string: ${body.getClass().getCanonicalName()}", e)
        return null
    }
}

/**
 * Convert Axway HeaderSet to Map<String, Collection<String>>.
 *
 * @param headers The Axway header object
 * @return A map of header names to values
 */
def convertHeaders(headers) {
    def headerMap = new HashMap<String, Collection<String>>()
    if (headers == null) {
        return headerMap
    }

    try {
        // Handle HeaderSet type from Axway
        def headerNames = headers.getHeaderNames()
        while (headerNames.hasNext()) {
            def name = headerNames.next()
            def values = headers.getHeaders(name)
            if (values != null) {
                headerMap.put(name, new ArrayList<String>(values))
            }
        }
    } catch (Exception e) {
        // Fallback: try to iterate as a map
        try {
            headers.each { key, value ->
                if (value instanceof Collection) {
                    headerMap.put(key.toString(), new ArrayList<String>(value))
                } else {
                    headerMap.put(key.toString(), [value.toString()])
                }
            }
        } catch (Exception e2) {
            Trace.error("BnppfRequestValidation: Could not convert headers", e2)
        }
    }

    return headerMap
}

/**
 * Convert Axway query parameters to Map<String, Collection<String>>.
 *
 * @param queryParams The Axway query parameters object
 * @return A map of parameter names to values
 */
def convertQueryParams(queryParams) {
    def paramMap = new HashMap<String, Collection<String>>()
    if (queryParams == null) {
        return paramMap
    }

    try {
        queryParams.each { key, value ->
            if (value instanceof Collection) {
                paramMap.put(key.toString(), new ArrayList<String>(value))
            } else if (value != null) {
                paramMap.put(key.toString(), [value.toString()])
            }
        }
    } catch (Exception e) {
        Trace.error("BnppfRequestValidation: Could not convert query parameters", e)
    }

    return paramMap
}
