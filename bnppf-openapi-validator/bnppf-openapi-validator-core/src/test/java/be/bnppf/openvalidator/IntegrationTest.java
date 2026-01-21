package be.bnppf.openvalidator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using the full Petstore specification.
 */
@DisplayName("Integration Tests")
class IntegrationTest {

    private static BnppfOpenAPIValidator validator;
    private static String petstoreSpec;

    @BeforeAll
    static void setup() throws IOException {
        // Load the petstore specification
        try (InputStream is = IntegrationTest.class.getResourceAsStream("/petstore-openapi3.yaml")) {
            if (is == null) {
                throw new IOException("Could not find petstore-openapi3.yaml");
            }
            petstoreSpec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        BnppfOpenAPIValidator.clearAllInstances();
        validator = BnppfOpenAPIValidator.getInstance(petstoreSpec, ValidationLevel.STRICT);
    }

    @AfterAll
    static void cleanup() {
        BnppfOpenAPIValidator.clearAllInstances();
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Valid GET request to /pets returns valid")
        void validGetPets_ReturnsValid() {
            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/pets",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GET /pets with valid query params returns valid")
        void getPetsWithValidQueryParams_ReturnsValid() {
            Map<String, Collection<String>> queryParams = new HashMap<>();
            queryParams.put("limit", Collections.singletonList("10"));
            queryParams.put("offset", Collections.singletonList("0"));
            queryParams.put("status", Collections.singletonList("available"));

            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/pets",
                    queryParams,
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GET /pets with invalid query param value returns invalid")
        void getPetsWithInvalidQueryParam_ReturnsInvalid() {
            Map<String, Collection<String>> queryParams = new HashMap<>();
            queryParams.put("status", Collections.singletonList("invalid-status"));

            ValidationReport report = validator.validateRequest(
                    null,
                    "GET",
                    "/pets",
                    queryParams,
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("Valid POST request to /pets returns valid")
        void validPostPet_ReturnsValid() {
            String body = "{\"name\": \"Fluffy\", \"status\": \"available\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST /pets missing required field returns invalid")
        void postPetMissingRequired_ReturnsInvalid() {
            String body = "{\"status\": \"available\"}";  // Missing required 'name'
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
            assertThat(report.getErrorsAsString()).containsIgnoringCase("name");
        }

        @Test
        @DisplayName("POST /pets with wrong type returns invalid")
        void postPetWrongType_ReturnsInvalid() {
            String body = "{\"name\": 12345}";  // name should be string, not number
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("POST /pets with invalid enum value returns invalid")
        void postPetInvalidEnum_ReturnsInvalid() {
            String body = "{\"name\": \"Fluffy\", \"status\": \"invalid-status\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("GET /pets/{petId} with valid path parameter returns valid")
        void getPetById_ValidPathParam_ReturnsValid() {
            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/pets/123",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("PUT /pets/{petId} with valid body returns valid")
        void putPet_ValidBody_ReturnsValid() {
            String body = "{\"name\": \"Fluffy Updated\", \"status\": \"sold\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "PUT",
                    "/pets/123",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("DELETE /pets/{petId} with required header returns valid")
        void deletePet_WithRequiredHeader_ReturnsValid() {
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("X-Api-Key", Collections.singletonList("my-api-key-12345"));

            boolean result = validator.isValidRequest(
                    null,
                    "DELETE",
                    "/pets/123",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("DELETE /pets/{petId} without required header returns invalid")
        void deletePet_MissingRequiredHeader_ReturnsInvalid() {
            ValidationReport report = validator.validateRequest(
                    null,
                    "DELETE",
                    "/pets/123",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("Request to undefined path returns invalid")
        void undefinedPath_ReturnsInvalid() {
            ValidationReport report = validator.validateRequest(
                    null,
                    "GET",
                    "/undefined/path",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("Request with undefined method returns invalid")
        void undefinedMethod_ReturnsInvalid() {
            ValidationReport report = validator.validateRequest(
                    null,
                    "OPTIONS",
                    "/pets",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Response Validation Tests")
    class ResponseValidationTests {

        @Test
        @DisplayName("Valid GET /pets response returns valid")
        void validGetPetsResponse_ReturnsValid() {
            String body = "[{\"id\": 1, \"name\": \"Fluffy\", \"status\": \"available\"}]";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidResponse(
                    body,
                    "GET",
                    "/pets",
                    200,
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Valid POST /pets response returns valid")
        void validPostPetResponse_ReturnsValid() {
            String body = "{\"id\": 1, \"name\": \"Fluffy\", \"status\": \"available\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidResponse(
                    body,
                    "POST",
                    "/pets",
                    201,
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GET /pets response with wrong schema returns invalid")
        void getPetsResponseWrongSchema_ReturnsInvalid() {
            String body = "{\"id\": 1, \"name\": \"Fluffy\"}";  // Should be array, not object
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateResponse(
                    body,
                    "GET",
                    "/pets",
                    200,
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("Error response with valid error schema returns valid")
        void errorResponse_ValidSchema_ReturnsValid() {
            String body = "{\"code\": 400, \"message\": \"Bad request\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidResponse(
                    body,
                    "GET",
                    "/pets",
                    400,
                    headers
            );

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Nested Object Validation Tests")
    class NestedObjectTests {

        @Test
        @DisplayName("POST /pets with valid nested category returns valid")
        void postPetWithValidCategory_ReturnsValid() {
            String body = "{\"name\": \"Fluffy\", \"category\": {\"id\": 1, \"name\": \"Dogs\"}}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST /pets with invalid nested category returns invalid")
        void postPetWithInvalidCategory_ReturnsInvalid() {
            String body = "{\"name\": \"Fluffy\", \"category\": {\"id\": \"not-a-number\", \"name\": \"Dogs\"}}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Array Validation Tests")
    class ArrayValidationTests {

        @Test
        @DisplayName("POST /pets with valid tags array returns valid")
        void postPetWithValidTags_ReturnsValid() {
            String body = "{\"name\": \"Fluffy\", \"tags\": [{\"id\": 1, \"name\": \"cute\"}, {\"id\": 2, \"name\": \"fluffy\"}]}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GET /pets with array query parameter returns valid")
        void getPetsWithArrayQueryParam_ReturnsValid() {
            Map<String, Collection<String>> queryParams = new HashMap<>();
            queryParams.put("tags", Arrays.asList("cute", "small"));

            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/pets",
                    queryParams,
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Content-Type Validation Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("POST request with correct Content-Type returns valid")
        void postWithCorrectContentType_ReturnsValid() {
            String body = "{\"name\": \"Fluffy\"}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST request with charset in Content-Type returns valid")
        void postWithCharsetContentType_ReturnsValid() {
            String body = "{\"name\": \"Fluffy\"}";
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Order API Tests")
    class OrderApiTests {

        @Test
        @DisplayName("POST /orders with valid order returns valid")
        void postOrder_ValidBody_ReturnsValid() {
            String body = "{\"petId\": 1, \"quantity\": 1}";
            Map<String, Collection<String>> headers = createJsonHeaders();

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/orders",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST /orders with quantity exceeding max returns invalid")
        void postOrder_QuantityExceedsMax_ReturnsInvalid() {
            String body = "{\"petId\": 1, \"quantity\": 150}";  // max is 100
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/orders",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Store API Tests")
    class StoreApiTests {

        @Test
        @DisplayName("GET /stores with valid country pattern returns valid")
        void getStores_ValidCountry_ReturnsValid() {
            Map<String, Collection<String>> queryParams = new HashMap<>();
            queryParams.put("country", Collections.singletonList("US"));

            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/stores",
                    queryParams,
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("GET /stores with invalid country pattern returns invalid")
        void getStores_InvalidCountry_ReturnsInvalid() {
            Map<String, Collection<String>> queryParams = new HashMap<>();
            queryParams.put("country", Collections.singletonList("USA"));  // Should be 2 chars

            ValidationReport report = validator.validateRequest(
                    null,
                    "GET",
                    "/stores",
                    queryParams,
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Swagger 2.0 Compatibility Tests")
    class Swagger2CompatibilityTests {

        private BnppfOpenAPIValidator swagger2Validator;

        @Test
        @DisplayName("Can load and validate against Swagger 2.0 spec")
        void canLoadSwagger2Spec() throws IOException {
            try (InputStream is = IntegrationTest.class.getResourceAsStream("/sample-swagger2.json")) {
                if (is == null) {
                    throw new IOException("Could not find sample-swagger2.json");
                }
                String swagger2Spec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                swagger2Validator = BnppfOpenAPIValidator.getInstance(swagger2Spec, ValidationLevel.STRICT);

                assertThat(swagger2Validator.isSpecificationValid()).isTrue();
            }
        }

        @Test
        @DisplayName("Swagger 2.0 spec validates requests correctly")
        void swagger2_ValidatesRequests() throws IOException {
            try (InputStream is = IntegrationTest.class.getResourceAsStream("/sample-swagger2.json")) {
                if (is == null) {
                    throw new IOException("Could not find sample-swagger2.json");
                }
                String swagger2Spec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                swagger2Validator = BnppfOpenAPIValidator.getInstance(swagger2Spec, ValidationLevel.STRICT);

                // Valid request
                String body = "{\"username\": \"testuser\", \"email\": \"test@example.com\", \"password\": \"password123\"}";
                Map<String, Collection<String>> headers = createJsonHeaders();

                boolean result = swagger2Validator.isValidRequest(
                        body,
                        "POST",
                        "/users",
                        Collections.emptyMap(),
                        headers
                );

                assertThat(result).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Validation Report Tests")
    class ValidationReportTests {

        @Test
        @DisplayName("ValidationReport provides detailed error information")
        void validationReport_ProvidesDetailedInfo() {
            String body = "{\"status\": \"invalid\"}";  // Missing 'name', invalid 'status'
            Map<String, Collection<String>> headers = createJsonHeaders();

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/pets",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
            assertThat(report.getErrors()).isNotEmpty();
            assertThat(report.getMessagesAsString()).isNotEmpty();
            assertThat(report.toJson()).contains("\"valid\":false");
        }

        @Test
        @DisplayName("Empty ValidationReport is valid")
        void emptyReport_IsValid() {
            ValidationReport report = ValidationReport.empty();

            assertThat(report.isValid()).isTrue();
            assertThat(report.hasErrors()).isFalse();
            assertThat(report.hasWarnings()).isFalse();
            assertThat(report.getMessages()).isEmpty();
        }

        @Test
        @DisplayName("ValidationReport merge combines multiple reports")
        void validationReport_MergeWorks() {
            ValidationReport report1 = new ValidationReport()
                    .addError("error1", "First error", "/path1");
            ValidationReport report2 = new ValidationReport()
                    .addWarning("warn1", "First warning", "/path2");

            ValidationReport merged = ValidationReport.merge(report1, report2);

            assertThat(merged.hasErrors()).isTrue();
            assertThat(merged.hasWarnings()).isTrue();
            assertThat(merged.getMessages()).hasSize(2);
        }
    }

    private static Map<String, Collection<String>> createJsonHeaders() {
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        return headers;
    }
}
