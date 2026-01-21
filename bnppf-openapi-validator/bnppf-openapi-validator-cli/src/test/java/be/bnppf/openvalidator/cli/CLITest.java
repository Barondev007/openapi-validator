package be.bnppf.openvalidator.cli;

import be.bnppf.openvalidator.BnppfOpenAPIValidator;
import be.bnppf.openvalidator.cli.output.OutputFormat;
import be.bnppf.openvalidator.cli.output.ReportFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the CLI commands.
 */
@DisplayName("CLI Tests")
class CLITest {

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

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @TempDir
    Path tempDir;

    private Path specFile;

    @BeforeEach
    void setup() throws IOException {
        // Capture stdout and stderr
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Create temp spec file
        specFile = tempDir.resolve("test-spec.yaml");
        Files.write(specFile, SIMPLE_SPEC.getBytes(StandardCharsets.UTF_8));

        // Clear validator cache
        BnppfOpenAPIValidator.clearAllInstances();
    }

    @AfterEach
    void cleanup() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        BnppfOpenAPIValidator.clearAllInstances();
    }

    @Nested
    @DisplayName("Main CLI Tests")
    class MainCLITests {

        @Test
        @DisplayName("Running without arguments shows help")
        void noArguments_ShowsHelp() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI()).execute();

            assertThat(exitCode).isEqualTo(0);
            String output = outContent.toString();
            assertThat(output).contains("bnppf-openapi-validator");
            assertThat(output).contains("validate-spec");
            assertThat(output).contains("validate-request");
            assertThat(output).contains("validate-response");
        }

        @Test
        @DisplayName("--help shows help")
        void helpOption_ShowsHelp() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI()).execute("--help");

            assertThat(exitCode).isEqualTo(0);
            String output = outContent.toString();
            assertThat(output).contains("Usage:");
        }

        @Test
        @DisplayName("--version shows version")
        void versionOption_ShowsVersion() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI()).execute("--version");

            assertThat(exitCode).isEqualTo(0);
            String output = outContent.toString();
            assertThat(output).contains("1.0.0");
        }
    }

    @Nested
    @DisplayName("ValidateSpecCommand Tests")
    class ValidateSpecCommandTests {

        @Test
        @DisplayName("Valid spec file returns exit code 0")
        void validSpecFile_ReturnsZero() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-spec", "--spec", specFile.toString());

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Missing spec option shows error")
        void missingSpecOption_ShowsError() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-spec");

            assertThat(exitCode).isNotEqualTo(0);
        }

        @Test
        @DisplayName("Non-existent spec file returns error")
        void nonExistentSpecFile_ReturnsError() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-spec", "--spec", "/non/existent/file.yaml");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("Output format JSON produces JSON")
        void outputFormatJson_ProducesJson() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-spec", "--spec", specFile.toString(), "--output", "json");

            assertThat(exitCode).isEqualTo(0);
            String output = outContent.toString();
            assertThat(output).contains("{");
            assertThat(output).contains("\"valid\"");
        }
    }

    @Nested
    @DisplayName("ValidateRequestCommand Tests")
    class ValidateRequestCommandTests {

        @Test
        @DisplayName("Valid GET request returns exit code 0")
        void validGetRequest_ReturnsZero() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/test");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Valid POST request with body returns exit code 0")
        void validPostRequest_ReturnsZero() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "POST",
                            "--path", "/test",
                            "--body", "{\"name\": \"test\"}",
                            "--content-type", "application/json");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Invalid POST request missing required field returns error")
        void invalidPostRequest_ReturnsError() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "POST",
                            "--path", "/test",
                            "--body", "{\"value\": 123}",
                            "--content-type", "application/json",
                            "--level", "strict");

            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        @DisplayName("Request with body file works")
        void requestWithBodyFile_Works() throws IOException {
            Path bodyFile = tempDir.resolve("body.json");
            Files.write(bodyFile, "{\"name\": \"test\"}".getBytes(StandardCharsets.UTF_8));

            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "POST",
                            "--path", "/test",
                            "--body-file", bodyFile.toString(),
                            "--content-type", "application/json");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Request with headers works")
        void requestWithHeaders_Works() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/test",
                            "--header", "X-Custom-Header: value",
                            "--header", "Authorization: Bearer token");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Request with query parameters works")
        void requestWithQueryParams_Works() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/test",
                            "--query", "param1=value1",
                            "--query", "param2=value2");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Undefined path returns error")
        void undefinedPath_ReturnsError() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-request",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/undefined");

            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ValidateResponseCommand Tests")
    class ValidateResponseCommandTests {

        @Test
        @DisplayName("Valid response returns exit code 0")
        void validResponse_ReturnsZero() {
            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-response",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/test",
                            "--status", "200",
                            "--body", "{\"message\": \"hello\"}",
                            "--content-type", "application/json");

            assertThat(exitCode).isEqualTo(0);
        }

        @Test
        @DisplayName("Response with body file works")
        void responseWithBodyFile_Works() throws IOException {
            Path bodyFile = tempDir.resolve("response.json");
            Files.write(bodyFile, "{\"message\": \"hello\"}".getBytes(StandardCharsets.UTF_8));

            int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                    .execute("validate-response",
                            "--spec", specFile.toString(),
                            "--method", "GET",
                            "--path", "/test",
                            "--status", "200",
                            "--body-file", bodyFile.toString(),
                            "--content-type", "application/json");

            assertThat(exitCode).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("OutputFormat Tests")
    class OutputFormatTests {

        @Test
        @DisplayName("TEXT format is default")
        void textFormatIsDefault() {
            assertThat(OutputFormat.fromString(null)).isEqualTo(OutputFormat.TEXT);
            assertThat(OutputFormat.fromString("")).isEqualTo(OutputFormat.TEXT);
        }

        @Test
        @DisplayName("fromString parses correctly")
        void fromStringParsesCorrectly() {
            assertThat(OutputFormat.fromString("text")).isEqualTo(OutputFormat.TEXT);
            assertThat(OutputFormat.fromString("TEXT")).isEqualTo(OutputFormat.TEXT);
            assertThat(OutputFormat.fromString("json")).isEqualTo(OutputFormat.JSON);
            assertThat(OutputFormat.fromString("JSON")).isEqualTo(OutputFormat.JSON);
            assertThat(OutputFormat.fromString("junit")).isEqualTo(OutputFormat.JUNIT);
            assertThat(OutputFormat.fromString("JUNIT")).isEqualTo(OutputFormat.JUNIT);
        }

        @Test
        @DisplayName("Unknown format defaults to TEXT")
        void unknownFormatDefaultsToText() {
            assertThat(OutputFormat.fromString("unknown")).isEqualTo(OutputFormat.TEXT);
        }
    }

    @Nested
    @DisplayName("ReportFormatter Tests")
    class ReportFormatterTests {

        @Test
        @DisplayName("formatResult formats success correctly")
        void formatResult_Success() {
            ReportFormatter formatter = new ReportFormatter(false);
            String result = formatter.formatResult(true, "Validation passed");

            assertThat(result).contains("Validation passed");
        }

        @Test
        @DisplayName("formatResult formats failure correctly")
        void formatResult_Failure() {
            ReportFormatter formatter = new ReportFormatter(false);
            String result = formatter.formatResult(false, "Validation failed");

            assertThat(result).contains("Validation failed");
        }

        @Test
        @DisplayName("formatError formats correctly")
        void formatError() {
            ReportFormatter formatter = new ReportFormatter(false);
            String result = formatter.formatError("Something went wrong");

            assertThat(result).contains("Error");
            assertThat(result).contains("Something went wrong");
        }
    }
}
