/**
 * Groovy script for Axway API Gateway response validation.
 *
 * This script validates outgoing HTTP responses against an OpenAPI specification.
 * It should be used in a Scripting Filter within Axway Policy Studio, typically
 * placed after the backend service call.
 *
 * Required Axway message attributes:
 * - specFile: Path to the OpenAPI specification file (required)
 * - validationLevel: Validation strictness - light, lenient, or strict (optional, default: lenient)
 * - validationDebug: Enable debug logging - true/false (optional, default: false)
 *
 * Output:
 * - Sets 'validation.result' to true/false
 * - Sets 'validation.errors' with error messages if validation fails
 * - Does NOT modify http.response.status (to preserve backend response)
 *
 * Note: Response validation typically does not block the response to the client.
 * Instead, it logs validation errors and can be used for monitoring and alerting.
 * Set the policy to continue on failure if you want to pass through invalid responses.
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
 * @param msg The Axway message object containing response data and attributes
 * @return true if validation passes, false otherwise
 */
def invoke(msg) {
    try {
        // Get the specification file path from Axway variable
        def specFile = msg.get("specFile")
        if (specFile == null || specFile.toString().trim().isEmpty()) {
            Trace.error("BnppfResponseValidation: specFile variable is not set")
            msg.put("validation.result", false)
            msg.put("validation.errors", "OpenAPI specification file not configured")
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

        // Get response details from the message
        def payload = bodyAsString(msg.get('content.body'))
        def path = msg.get("http.request.path")?.toString() ?: ""
        def verb = msg.get("http.request.verb")?.toString()?.toUpperCase() ?: "GET"
        def status = getStatusCode(msg.get("http.response.status"))

        // Get response headers
        def headers = msg.get("http.headers")
        def contentHeaders = msg.get("http.content.headers")

        if (debug) {
            Trace.info("BnppfResponseValidation: Validating response [path: ${path}, method: ${verb}, status: ${status}, level: ${validationLevel}]")
        }

        // Merge Content-Type header from content headers if available
        if (contentHeaders != null && headers != null) {
            try {
                headers.addHeaders(contentHeaders)
            } catch (Exception e) {
                if (debug) {
                    Trace.info("BnppfResponseValidation: Could not merge content headers: ${e.message}")
                }
            }
        }

        // Convert Axway HeaderSet to Map
        def headerMap = convertHeaders(headers)

        // Perform validation with detailed report
        def validationReport = validator.validateResponse(payload, verb, path, status, headerMap)

        if (validationReport.hasErrors()) {
            def errorMessages = validationReport.getMessagesAsString()

            if (debug) {
                Trace.info("BnppfResponseValidation: Response validation failed: ${errorMessages}")
            }

            // Log the validation failure (but don't block the response by default)
            Trace.error("BnppfResponseValidation: Invalid response for ${verb} ${path} (${status}): ${errorMessages}")

            msg.put("validation.result", false)
            msg.put("validation.errors", errorMessages)
            msg.put("validation.report.json", validationReport.toJson())

            // Note: We don't set circuit.failure.reason or modify http.response.status
            // to allow the original response to pass through.
            // Configure your policy to handle validation failures as needed.

            return false
        }

        if (debug && validationReport.hasWarnings()) {
            Trace.info("BnppfResponseValidation: Response validation warnings: ${validationReport.getWarnings()}")
        }

        msg.put("validation.result", true)
        msg.put("validation.errors", "")

        return true

    } catch (IllegalArgumentException e) {
        Trace.error("BnppfResponseValidation: Configuration error - ${e.message}")
        msg.put("validation.result", false)
        msg.put("validation.errors", "Validation configuration error: ${e.message}")
        return false
    } catch (Exception e) {
        Trace.error("BnppfResponseValidation: Error during response validation", e)
        msg.put("validation.result", false)
        msg.put("validation.errors", "Internal validation error: ${e.message}")
        return false
    }
}

/**
 * Convert the response body to a String.
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
        Trace.error("BnppfResponseValidation: Error converting body to string: ${body.getClass().getCanonicalName()}", e)
        return null
    }
}

/**
 * Get the HTTP status code as an integer.
 *
 * @param status The status code object from the message
 * @return The status code as an integer, defaults to 200
 */
def getStatusCode(status) {
    if (status == null) {
        return 200
    }
    try {
        if (status instanceof Integer) {
            return status
        }
        return Integer.parseInt(status.toString())
    } catch (NumberFormatException e) {
        Trace.error("BnppfResponseValidation: Invalid status code: ${status}")
        return 200
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
            Trace.error("BnppfResponseValidation: Could not convert headers", e2)
        }
    }

    return headerMap
}
