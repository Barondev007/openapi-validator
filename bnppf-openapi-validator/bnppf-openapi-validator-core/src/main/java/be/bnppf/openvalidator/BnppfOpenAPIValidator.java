package be.bnppf.openvalidator;

import be.bnppf.openvalidator.cache.LRUCache;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest.Builder;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport.Level;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAPI/Swagger validator for Axway API Gateway.
 * Validates requests and responses against OpenAPI 2.0 (Swagger) and OpenAPI 3.x specifications.
 *
 * <p>NO dependency on Axway API Manager - specification must be provided externally.
 *
 * <p>This class follows a singleton pattern per specification. Multiple calls with the same
 * specification will return the same validator instance.
 *
 * <p>Example usage:
 * <pre>{@code
 * // From inline specification
 * BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(specContent);
 * validator.setValidationLevel(ValidationLevel.STRICT);
 *
 * // Validate a request
 * boolean isValid = validator.isValidRequest(body, "POST", "/pets", queryParams, headers);
 *
 * // Get detailed validation report
 * ValidationReport report = validator.validateRequest(body, "POST", "/pets", queryParams, headers);
 * if (report.hasErrors()) {
 *     System.err.println(report.getErrorsAsString());
 * }
 * }</pre>
 */
public class BnppfOpenAPIValidator {

    private static final Logger log = LoggerFactory.getLogger(BnppfOpenAPIValidator.class);

    // Cache of validator instances keyed by specification hash
    private static final Map<String, BnppfOpenAPIValidator> instances = new ConcurrentHashMap<>();

    // Instance fields
    private final String specHash;
    private final String specContent;
    private final OpenAPI openAPI;
    private OpenApiInteractionValidator validator;
    private ValidationLevel validationLevel;
    private boolean debug;
    private boolean decodeQueryParams;
    private final LRUCache<String, String> exposurePath2SpecifiedPathMap;
    private List<String> specificationErrors;

    /**
     * Private constructor - use factory methods.
     */
    private BnppfOpenAPIValidator(String specContent, String specHash, ValidationLevel validationLevel) {
        this.specContent = specContent;
        this.specHash = specHash;
        this.validationLevel = validationLevel != null ? validationLevel : ValidationLevel.LENIENT;
        this.debug = false;
        this.decodeQueryParams = true;
        this.exposurePath2SpecifiedPathMap = new LRUCache<>(1000);
        this.specificationErrors = new ArrayList<>();

        // Parse the OpenAPI specification
        this.openAPI = parseSpecification(specContent);

        // Build the validator
        this.validator = buildValidator();
    }

    // ========== FACTORY METHODS ==========

    /**
     * Get or create a validator instance from an inline specification string.
     *
     * @param specContent The OpenAPI/Swagger specification as JSON or YAML string
     * @return BnppfOpenAPIValidator instance
     * @throws IllegalArgumentException if specContent is null or empty
     */
    public static BnppfOpenAPIValidator getInstance(String specContent) {
        return getInstance(specContent, ValidationLevel.LENIENT);
    }

    /**
     * Get or create a validator instance from an inline specification with validation level.
     *
     * @param specContent     The OpenAPI/Swagger specification as JSON or YAML string
     * @param validationLevel The validation strictness level
     * @return BnppfOpenAPIValidator instance
     * @throws IllegalArgumentException if specContent is null or empty
     */
    public static BnppfOpenAPIValidator getInstance(String specContent, ValidationLevel validationLevel) {
        if (specContent == null || specContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Specification content cannot be null or empty");
        }

        String hash = computeHash(specContent);
        return instances.computeIfAbsent(hash, h -> new BnppfOpenAPIValidator(specContent, h, validationLevel));
    }

    /**
     * Clear all cached validator instances.
     */
    public static void clearAllInstances() {
        instances.clear();
        log.debug("All validator instances cleared");
    }

    /**
     * Remove a specific validator instance from cache.
     *
     * @param specHash the hash of the specification to remove
     */
    public static void removeInstance(String specHash) {
        if (specHash != null) {
            instances.remove(specHash);
            log.debug("Validator instance removed for hash: {}", specHash);
        }
    }

    /**
     * Get the number of cached validator instances.
     *
     * @return the number of cached instances
     */
    public static int getInstanceCount() {
        return instances.size();
    }

    // ========== CONFIGURATION ==========

    /**
     * Set the validation level for this validator instance.
     * This will rebuild the internal validator.
     *
     * @param level ValidationLevel (LIGHT, LENIENT, STRICT)
     */
    public void setValidationLevel(ValidationLevel level) {
        if (level != null && level != this.validationLevel) {
            this.validationLevel = level;
            this.validator = buildValidator();
            debugLog("Validation level changed to: {}", level);
        }
    }

    /**
     * Get the current validation level.
     *
     * @return the current ValidationLevel
     */
    public ValidationLevel getValidationLevel() {
        return validationLevel;
    }

    /**
     * Enable or disable debug mode.
     * When enabled, detailed validation information will be logged.
     *
     * @param debug true to enable debug logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Check if debug mode is enabled.
     *
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Enable or disable query parameter decoding.
     *
     * @param decode true to decode query parameters (default: true)
     */
    public void setDecodeQueryParams(boolean decode) {
        this.decodeQueryParams = decode;
    }

    /**
     * Check if query parameter decoding is enabled.
     *
     * @return true if query parameters will be decoded
     */
    public boolean isDecodeQueryParams() {
        return decodeQueryParams;
    }

    /**
     * Get the path-to-spec-path mapping cache for size configuration.
     *
     * @return the LRU cache for path mappings
     */
    public LRUCache<String, String> getExposurePath2SpecifiedPathMap() {
        return exposurePath2SpecifiedPathMap;
    }

    /**
     * Get the specification hash.
     *
     * @return the hash of the specification content
     */
    public String getSpecHash() {
        return specHash;
    }

    /**
     * Get the parsed OpenAPI object.
     *
     * @return the parsed OpenAPI specification
     */
    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    // ========== REQUEST VALIDATION ==========

    /**
     * Validate a request against the specification.
     *
     * @param payload     Request body (can be null for GET requests)
     * @param httpMethod  HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
     * @param path        Request path
     * @param queryParams Query parameters as Map&lt;String, Collection&lt;String&gt;&gt;
     * @param headers     HTTP headers
     * @return true if valid, false otherwise
     */
    public boolean isValidRequest(String payload, String httpMethod, String path,
                                  Map<String, Collection<String>> queryParams,
                                  Map<String, Collection<String>> headers) {
        ValidationReport report = validateRequest(payload, httpMethod, path, queryParams, headers);
        return !report.hasErrors();
    }

    /**
     * Validate a request and return detailed validation report.
     *
     * @param payload     Request body (can be null for GET requests)
     * @param httpMethod  HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
     * @param path        Request path
     * @param queryParams Query parameters as Map&lt;String, Collection&lt;String&gt;&gt;
     * @param headers     HTTP headers
     * @return ValidationReport with all validation messages
     */
    public ValidationReport validateRequest(String payload, String httpMethod, String path,
                                            Map<String, Collection<String>> queryParams,
                                            Map<String, Collection<String>> headers) {
        debugLog("Validating request: method={}, path={}, level={}", httpMethod, path, validationLevel);

        try {
            // Build the request
            Request request = buildRequest(payload, httpMethod, path, queryParams, headers);

            // Validate
            com.atlassian.oai.validator.report.ValidationReport atlassianReport =
                    validator.validateRequest(request);

            // Convert to our report format
            return convertReport(atlassianReport);

        } catch (Exception e) {
            log.error("Error during request validation", e);
            return ValidationReport.withError(
                    "validation.error",
                    "Internal validation error: " + e.getMessage(),
                    path
            );
        }
    }

    // ========== RESPONSE VALIDATION ==========

    /**
     * Validate a response against the specification.
     *
     * @param payload    Response body
     * @param httpMethod HTTP method used in the request
     * @param path       Request path
     * @param statusCode HTTP response status code
     * @param headers    Response headers
     * @return true if valid, false otherwise
     */
    public boolean isValidResponse(String payload, String httpMethod, String path,
                                   int statusCode, Map<String, Collection<String>> headers) {
        ValidationReport report = validateResponse(payload, httpMethod, path, statusCode, headers);
        return !report.hasErrors();
    }

    /**
     * Validate a response and return detailed validation report.
     *
     * @param payload    Response body
     * @param httpMethod HTTP method used in the request
     * @param path       Request path
     * @param statusCode HTTP response status code
     * @param headers    Response headers
     * @return ValidationReport with all validation messages
     */
    public ValidationReport validateResponse(String payload, String httpMethod, String path,
                                             int statusCode, Map<String, Collection<String>> headers) {
        debugLog("Validating response: method={}, path={}, status={}, level={}",
                httpMethod, path, statusCode, validationLevel);

        try {
            // Build the response
            Response response = buildResponse(payload, statusCode, headers);

            // Validate
            com.atlassian.oai.validator.report.ValidationReport atlassianReport =
                    validator.validateResponse(path, Request.Method.valueOf(httpMethod.toUpperCase()), response);

            // Convert to our report format
            return convertReport(atlassianReport);

        } catch (Exception e) {
            log.error("Error during response validation", e);
            return ValidationReport.withError(
                    "validation.error",
                    "Internal validation error: " + e.getMessage(),
                    path
            );
        }
    }

    // ========== SPECIFICATION VALIDATION ==========

    /**
     * Validate the OpenAPI specification itself (schema validation).
     *
     * @return ValidationReport with specification validation messages
     */
    public ValidationReport validateSpecification() {
        ValidationReport report = new ValidationReport();

        if (!specificationErrors.isEmpty()) {
            for (String error : specificationErrors) {
                report.addError("spec.parse.error", error, "specification");
            }
        }

        if (openAPI == null) {
            report.addError("spec.null", "OpenAPI specification could not be parsed", "specification");
        } else {
            // Additional validation checks
            if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
                report.addWarning("spec.no.paths", "Specification has no paths defined", "specification");
            }

            if (openAPI.getInfo() == null) {
                report.addWarning("spec.no.info", "Specification has no info section", "specification");
            }
        }

        return report;
    }

    /**
     * Check if the loaded specification is valid.
     *
     * @return true if specification is valid
     */
    public boolean isSpecificationValid() {
        return openAPI != null && specificationErrors.isEmpty();
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Parse the OpenAPI specification from content.
     */
    private OpenAPI parseSpecification(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            specificationErrors.addAll(result.getMessages());
            for (String msg : result.getMessages()) {
                log.warn("Specification parse warning: {}", msg);
            }
        }

        return result.getOpenAPI();
    }

    /**
     * Build the OpenApiInteractionValidator with the appropriate settings for the validation level.
     */
    private OpenApiInteractionValidator buildValidator() {
        OpenApiInteractionValidator.Builder builder =
                OpenApiInteractionValidator.createForInlineApiSpecification(specContent);

        // Configure based on validation level
        switch (validationLevel) {
            case LIGHT:
                builder.withLevelResolver(LevelResolver.create()
                        // Ignore additional properties
                        .withLevel("validation.request.body.schema.additionalProperties", Level.IGNORE)
                        .withLevel("validation.response.body.schema.additionalProperties", Level.IGNORE)
                        // Make required fields warnings instead of errors
                        .withLevel("validation.request.body.schema.required", Level.WARN)
                        .withLevel("validation.response.body.schema.required", Level.WARN)
                        // Ignore format validation
                        .withLevel("validation.request.body.schema.format", Level.IGNORE)
                        .withLevel("validation.response.body.schema.format", Level.IGNORE)
                        // Make type validation warnings
                        .withLevel("validation.request.body.schema.type", Level.WARN)
                        .withLevel("validation.response.body.schema.type", Level.WARN)
                        // Ignore enum validation
                        .withLevel("validation.request.body.schema.enum", Level.WARN)
                        .withLevel("validation.response.body.schema.enum", Level.WARN)
                        // Ignore pattern validation
                        .withLevel("validation.request.body.schema.pattern", Level.IGNORE)
                        .withLevel("validation.response.body.schema.pattern", Level.IGNORE)
                        // Ignore min/max validation
                        .withLevel("validation.request.body.schema.minimum", Level.IGNORE)
                        .withLevel("validation.request.body.schema.maximum", Level.IGNORE)
                        .withLevel("validation.request.body.schema.minLength", Level.IGNORE)
                        .withLevel("validation.request.body.schema.maxLength", Level.IGNORE)
                        .withLevel("validation.request.body.schema.minItems", Level.IGNORE)
                        .withLevel("validation.request.body.schema.maxItems", Level.IGNORE)
                        .build());
                break;

            case LENIENT:
                builder.withLevelResolver(LevelResolver.create()
                        // Allow additional properties
                        .withLevel("validation.request.body.schema.additionalProperties", Level.IGNORE)
                        .withLevel("validation.response.body.schema.additionalProperties", Level.IGNORE)
                        // Make format validation warnings
                        .withLevel("validation.request.body.schema.format", Level.WARN)
                        .withLevel("validation.response.body.schema.format", Level.WARN)
                        // Make pattern validation warnings
                        .withLevel("validation.request.body.schema.pattern", Level.WARN)
                        .withLevel("validation.response.body.schema.pattern", Level.WARN)
                        .build());
                break;

            case STRICT:
                // Use default strict validation - no overrides needed
                // All validations will be at ERROR level by default
                break;
        }

        return builder.build();
    }

    /**
     * Build a Request object for validation.
     */
    private Request buildRequest(String payload, String httpMethod, String path,
                                 Map<String, Collection<String>> queryParams,
                                 Map<String, Collection<String>> headers) {
        Request.Method method = Request.Method.valueOf(httpMethod.toUpperCase());

        Builder requestBuilder = new Builder(method, path);

        // Extract Content-Type from headers for body processing
        String contentType = null;
        if (headers != null) {
            for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                    Collection<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        contentType = values.iterator().next();
                    }
                    break;
                }
            }
        }

        // Add body if present with content type
        if (payload != null && !payload.isEmpty()) {
            if (contentType != null) {
                requestBuilder.withBody(payload).withContentType(contentType);
            } else {
                requestBuilder.withBody(payload);
            }
        }

        // Add query parameters
        if (queryParams != null) {
            for (Map.Entry<String, Collection<String>> entry : queryParams.entrySet()) {
                String paramName = entry.getKey();
                Collection<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        String decodedValue = decodeQueryParams ? decodeUrlParameter(value) : value;
                        requestBuilder.withQueryParam(paramName, decodedValue);
                    }
                }
            }
        }

        // Add headers
        if (headers != null) {
            for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                Collection<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        requestBuilder.withHeader(headerName, value);
                    }
                }
            }
        }

        return requestBuilder.build();
    }

    /**
     * Build a Response object for validation.
     */
    private Response buildResponse(String payload, int statusCode,
                                   Map<String, Collection<String>> headers) {
        SimpleResponse.Builder responseBuilder = new SimpleResponse.Builder(statusCode);

        // Extract Content-Type from headers for body processing
        String contentType = null;
        if (headers != null) {
            for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                    Collection<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        contentType = values.iterator().next();
                    }
                    break;
                }
            }
        }

        // Add body if present with content type
        if (payload != null && !payload.isEmpty()) {
            if (contentType != null) {
                responseBuilder.withBody(payload).withContentType(contentType);
            } else {
                responseBuilder.withBody(payload);
            }
        }

        // Add headers
        if (headers != null) {
            for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                Collection<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        responseBuilder.withHeader(headerName, value);
                    }
                }
            }
        }

        return responseBuilder.build();
    }

    /**
     * Convert Atlassian ValidationReport to our ValidationReport.
     */
    private ValidationReport convertReport(com.atlassian.oai.validator.report.ValidationReport atlassianReport) {
        ValidationReport report = new ValidationReport();

        if (atlassianReport.hasErrors()) {
            for (com.atlassian.oai.validator.report.ValidationReport.Message msg : atlassianReport.getMessages()) {
                ValidationReport.Level level = convertLevel(msg.getLevel());
                List<String> additionalInfo = new ArrayList<>();
                if (msg.getAdditionalInfo() != null) {
                    additionalInfo.addAll(msg.getAdditionalInfo());
                }

                report.addMessage(ValidationReport.Message.builder()
                        .level(level)
                        .key(msg.getKey())
                        .message(msg.getMessage())
                        .context(msg.getContext().isPresent() ?
                                msg.getContext().get().toString() : null)
                        .additionalInfo(additionalInfo)
                        .build());
            }
        }

        return report;
    }

    /**
     * Convert Atlassian Level to our Level.
     */
    private ValidationReport.Level convertLevel(Level atlassianLevel) {
        switch (atlassianLevel) {
            case ERROR:
                return ValidationReport.Level.ERROR;
            case WARN:
                return ValidationReport.Level.WARN;
            case INFO:
                return ValidationReport.Level.INFO;
            case IGNORE:
                return ValidationReport.Level.IGNORE;
            default:
                return ValidationReport.Level.INFO;
        }
    }

    /**
     * URL-decode a parameter value.
     */
    private String decodeUrlParameter(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Should never happen with UTF-8
            return value;
        }
    }

    /**
     * Compute a hash of the specification content for caching.
     */
    private static String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to hashCode if SHA-256 is not available (should never happen)
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * Log a debug message if debug mode is enabled.
     */
    private void debugLog(String message, Object... args) {
        if (debug) {
            log.info("[DEBUG] " + message, args);
        }
    }
}
