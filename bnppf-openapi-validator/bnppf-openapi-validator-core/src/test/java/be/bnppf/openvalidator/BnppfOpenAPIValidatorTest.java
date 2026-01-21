package be.bnppf.openvalidator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BnppfOpenAPIValidator.
 */
@DisplayName("BnppfOpenAPIValidator Tests")
class BnppfOpenAPIValidatorTest {

    private static final String SIMPLE_SPEC = "openapi: '3.0.3'\n" +
            "info:\n" +
            "  title: Test API\n" +
            "  version: '1.0.0'\n" +
            "paths:\n" +
            "  /test:\n" +
            "    get:\n" +
            "      summary: Test endpoint\n" +
            "      responses:\n" +
            "        '200':\n" +
            "          description: Success\n" +
            "          content:\n" +
            "            application/json:\n" +
            "              schema:\n" +
            "                type: object\n" +
            "                properties:\n" +
            "                  message:\n" +
            "                    type: string\n" +
            "    post:\n" +
            "      summary: Create test\n" +
            "      requestBody:\n" +
            "        required: true\n" +
            "        content:\n" +
            "          application/json:\n" +
            "            schema:\n" +
            "              type: object\n" +
            "              required:\n" +
            "                - name\n" +
            "              properties:\n" +
            "                name:\n" +
            "                  type: string\n" +
            "                value:\n" +
            "                  type: integer\n" +
            "      responses:\n" +
            "        '201':\n" +
            "          description: Created\n";

    @AfterEach
    void cleanup() {
        BnppfOpenAPIValidator.clearAllInstances();
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("getInstance with valid spec content returns validator")
        void getInstance_ValidSpec_ReturnsValidator() {
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);

            assertThat(validator).isNotNull();
            assertThat(validator.isSpecificationValid()).isTrue();
            assertThat(validator.getValidationLevel()).isEqualTo(ValidationLevel.LENIENT);
        }

        @Test
        @DisplayName("getInstance with validation level sets correct level")
        void getInstance_WithValidationLevel_SetsCorrectLevel() {
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC, ValidationLevel.STRICT);

            assertThat(validator.getValidationLevel()).isEqualTo(ValidationLevel.STRICT);
        }

        @Test
        @DisplayName("getInstance with null spec throws exception")
        void getInstance_NullSpec_ThrowsException() {
            assertThatThrownBy(() -> BnppfOpenAPIValidator.getInstance(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("getInstance with empty spec throws exception")
        void getInstance_EmptySpec_ThrowsException() {
            assertThatThrownBy(() -> BnppfOpenAPIValidator.getInstance(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("getInstanceFromFile with valid file returns validator")
        void getInstanceFromFile_ValidFile_ReturnsValidator(@TempDir Path tempDir) throws IOException {
            Path specFile = tempDir.resolve("test-spec.yaml");
            Files.write(specFile, SIMPLE_SPEC.getBytes(StandardCharsets.UTF_8));

            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstanceFromFile(specFile.toString());

            assertThat(validator).isNotNull();
            assertThat(validator.isSpecificationValid()).isTrue();
        }

        @Test
        @DisplayName("getInstanceFromFile with null path throws exception")
        void getInstanceFromFile_NullPath_ThrowsException() {
            assertThatThrownBy(() -> BnppfOpenAPIValidator.getInstanceFromFile(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("getInstanceFromFile with non-existent file throws exception")
        void getInstanceFromFile_NonExistentFile_ThrowsException() {
            assertThatThrownBy(() -> BnppfOpenAPIValidator.getInstanceFromFile("/non/existent/file.yaml"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to read");
        }
    }

    @Nested
    @DisplayName("Singleton Behavior Tests")
    class SingletonBehaviorTests {

        @Test
        @DisplayName("Same spec returns same instance")
        void sameSpec_ReturnsSameInstance() {
            BnppfOpenAPIValidator validator1 = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
            BnppfOpenAPIValidator validator2 = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);

            assertThat(validator1).isSameAs(validator2);
        }

        @Test
        @DisplayName("Different specs return different instances")
        void differentSpecs_ReturnDifferentInstances() {
            String otherSpec = SIMPLE_SPEC.replace("Test API", "Other API");

            BnppfOpenAPIValidator validator1 = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
            BnppfOpenAPIValidator validator2 = BnppfOpenAPIValidator.getInstance(otherSpec);

            assertThat(validator1).isNotSameAs(validator2);
        }

        @Test
        @DisplayName("clearAllInstances removes all cached instances")
        void clearAllInstances_RemovesAllCachedInstances() {
            BnppfOpenAPIValidator validator1 = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
            String hash1 = validator1.getSpecHash();

            BnppfOpenAPIValidator.clearAllInstances();

            BnppfOpenAPIValidator validator2 = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);

            assertThat(validator2).isNotSameAs(validator1);
            assertThat(validator2.getSpecHash()).isEqualTo(hash1);
        }

        @Test
        @DisplayName("getInstanceCount returns correct count")
        void getInstanceCount_ReturnsCorrectCount() {
            assertThat(BnppfOpenAPIValidator.getInstanceCount()).isEqualTo(0);

            BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
            assertThat(BnppfOpenAPIValidator.getInstanceCount()).isEqualTo(1);

            String otherSpec = SIMPLE_SPEC.replace("Test API", "Other API");
            BnppfOpenAPIValidator.getInstance(otherSpec);
            assertThat(BnppfOpenAPIValidator.getInstanceCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        private BnppfOpenAPIValidator validator;

        @BeforeEach
        void setup() {
            validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
        }

        @Test
        @DisplayName("setValidationLevel changes level")
        void setValidationLevel_ChangesLevel() {
            assertThat(validator.getValidationLevel()).isEqualTo(ValidationLevel.LENIENT);

            validator.setValidationLevel(ValidationLevel.STRICT);
            assertThat(validator.getValidationLevel()).isEqualTo(ValidationLevel.STRICT);

            validator.setValidationLevel(ValidationLevel.LIGHT);
            assertThat(validator.getValidationLevel()).isEqualTo(ValidationLevel.LIGHT);
        }

        @Test
        @DisplayName("setDebug toggles debug mode")
        void setDebug_TogglesDebugMode() {
            assertThat(validator.isDebug()).isFalse();

            validator.setDebug(true);
            assertThat(validator.isDebug()).isTrue();

            validator.setDebug(false);
            assertThat(validator.isDebug()).isFalse();
        }

        @Test
        @DisplayName("setDecodeQueryParams toggles decoding")
        void setDecodeQueryParams_TogglesDecoding() {
            assertThat(validator.isDecodeQueryParams()).isTrue();

            validator.setDecodeQueryParams(false);
            assertThat(validator.isDecodeQueryParams()).isFalse();
        }

        @Test
        @DisplayName("getExposurePath2SpecifiedPathMap returns cache")
        void getExposurePath2SpecifiedPathMap_ReturnsCache() {
            assertThat(validator.getExposurePath2SpecifiedPathMap()).isNotNull();
            assertThat(validator.getExposurePath2SpecifiedPathMap().getMaxSize()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        private BnppfOpenAPIValidator validator;

        @BeforeEach
        void setup() {
            validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
        }

        @Test
        @DisplayName("Valid GET request returns true")
        void validGetRequest_ReturnsTrue() {
            boolean result = validator.isValidRequest(
                    null,
                    "GET",
                    "/test",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Valid POST request with body returns true")
        void validPostRequest_ReturnsTrue() {
            String body = "{\"name\": \"test\"}";
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json"));

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/test",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("POST request missing required field returns false with STRICT level")
        void postRequestMissingRequired_StrictLevel_ReturnsFalse() {
            validator.setValidationLevel(ValidationLevel.STRICT);
            String body = "{\"value\": 123}";  // Missing required 'name' field
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json"));

            boolean result = validator.isValidRequest(
                    body,
                    "POST",
                    "/test",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("validateRequest returns detailed report")
        void validateRequest_ReturnsDetailedReport() {
            validator.setValidationLevel(ValidationLevel.STRICT);
            String body = "{\"value\": \"not-a-number\"}";  // Wrong type for 'value'
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json"));

            ValidationReport report = validator.validateRequest(
                    body,
                    "POST",
                    "/test",
                    Collections.emptyMap(),
                    headers
            );

            assertThat(report.hasErrors()).isTrue();
            assertThat(report.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Request to undefined path returns error")
        void requestToUndefinedPath_ReturnsError() {
            ValidationReport report = validator.validateRequest(
                    null,
                    "GET",
                    "/undefined",
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );

            assertThat(report.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Response Validation Tests")
    class ResponseValidationTests {

        private BnppfOpenAPIValidator validator;

        @BeforeEach
        void setup() {
            validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);
        }

        @Test
        @DisplayName("Valid response returns true")
        void validResponse_ReturnsTrue() {
            String body = "{\"message\": \"Hello\"}";
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json"));

            boolean result = validator.isValidResponse(
                    body,
                    "GET",
                    "/test",
                    200,
                    headers
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("validateResponse returns detailed report")
        void validateResponse_ReturnsDetailedReport() {
            String body = "{\"message\": \"Hello\"}";
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Content-Type", Collections.singletonList("application/json"));

            ValidationReport report = validator.validateResponse(
                    body,
                    "GET",
                    "/test",
                    200,
                    headers
            );

            assertThat(report).isNotNull();
            assertThat(report.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Specification Validation Tests")
    class SpecificationValidationTests {

        @Test
        @DisplayName("Valid specification returns valid report")
        void validSpec_ReturnsValidReport() {
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);

            ValidationReport report = validator.validateSpecification();

            assertThat(report.hasErrors()).isFalse();
            assertThat(validator.isSpecificationValid()).isTrue();
        }

        @Test
        @DisplayName("getOpenAPI returns parsed specification")
        void getOpenAPI_ReturnsParsedSpec() {
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(SIMPLE_SPEC);

            assertThat(validator.getOpenAPI()).isNotNull();
            assertThat(validator.getOpenAPI().getInfo().getTitle()).isEqualTo("Test API");
        }
    }
}
