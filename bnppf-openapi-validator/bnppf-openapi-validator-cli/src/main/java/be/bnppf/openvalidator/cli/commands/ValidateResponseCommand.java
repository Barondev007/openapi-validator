package be.bnppf.openvalidator.cli.commands;

import be.bnppf.openvalidator.BnppfOpenAPIValidator;
import be.bnppf.openvalidator.ValidationLevel;
import be.bnppf.openvalidator.ValidationReport;
import be.bnppf.openvalidator.cli.output.OutputFormat;
import be.bnppf.openvalidator.cli.output.ReportFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to validate an HTTP response against an OpenAPI specification.
 */
@Command(
        name = "validate-response",
        description = "Validate an HTTP response against a specification",
        mixinStandardHelpOptions = true
)
public class ValidateResponseCommand implements Callable<Integer> {

    @Option(
            names = {"-s", "--spec"},
            required = true,
            description = "Path or URL to the OpenAPI specification"
    )
    private String specPath;

    @Option(
            names = {"-m", "--method"},
            required = true,
            description = "HTTP method used in the request (GET, POST, PUT, DELETE, PATCH, etc.)"
    )
    private String method;

    @Option(
            names = {"-p", "--path"},
            required = true,
            description = "Request path (e.g., /pets/123)"
    )
    private String path;

    @Option(
            names = {"--status"},
            required = true,
            description = "HTTP response status code"
    )
    private int statusCode;

    @Option(
            names = {"-b", "--body"},
            description = "Response body as string"
    )
    private String body;

    @Option(
            names = {"--body-file"},
            description = "Path to file containing response body"
    )
    private String bodyFile;

    @Option(
            names = {"--stdin"},
            description = "Read response body from stdin"
    )
    private boolean stdin;

    @Option(
            names = {"-H", "--header"},
            description = "HTTP header (can be repeated, format: 'Header-Name: value')"
    )
    private List<String> headers = new ArrayList<>();

    @Option(
            names = {"--content-type"},
            description = "Content-Type header (shorthand)"
    )
    private String contentType;

    @Option(
            names = {"-l", "--level"},
            description = "Validation level: light, lenient, strict (default: lenient)"
    )
    private String level = "lenient";

    @Option(
            names = {"-o", "--output"},
            description = "Output format: text, json, junit (default: text)"
    )
    private String outputFormat = "text";

    @Option(
            names = {"--no-color"},
            description = "Disable colored output"
    )
    private boolean noColor;

    @Option(
            names = {"--fail-on-warn"},
            description = "Exit with error code if warnings are found"
    )
    private boolean failOnWarn;

    @CommandLine.ParentCommand
    private Object parent;

    @Override
    public Integer call() {
        ReportFormatter formatter = new ReportFormatter(!noColor);
        OutputFormat format = OutputFormat.fromString(outputFormat);
        ValidationLevel validationLevel = ValidationLevel.fromString(level);

        try {
            // Resolve the response body
            String responseBody = resolveBody();

            // Parse headers
            Map<String, Collection<String>> headerMap = parseHeaders();

            // Load the specification content
            String specContent = loadSpecification(specPath);

            // Create validator from content string
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(specContent, validationLevel);

            // Validate the response
            ValidationReport report = validator.validateResponse(
                    responseBody,
                    method.toUpperCase(),
                    path,
                    statusCode,
                    headerMap
            );

            // Output the report
            String context = method.toUpperCase() + " " + path + " -> " + statusCode;
            System.out.println(formatter.format(report, format, context));

            // Determine exit code
            if (report.hasErrors()) {
                return 1;
            } else if (failOnWarn && report.hasWarnings()) {
                return 2;
            } else {
                return 0;
            }

        } catch (IllegalArgumentException e) {
            System.err.println(formatter.formatError(e.getMessage()));
            return 1;
        } catch (IOException e) {
            System.err.println(formatter.formatError("I/O error: " + e.getMessage()));
            return 1;
        } catch (Exception e) {
            System.err.println(formatter.formatError("Unexpected error: " + e.getMessage()));
            return 1;
        }
    }

    private String resolveBody() throws IOException {
        if (stdin) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String result = sb.toString().trim();
            return result.isEmpty() ? null : result;
        } else if (bodyFile != null && !bodyFile.isEmpty()) {
            return new String(Files.readAllBytes(Paths.get(bodyFile)), StandardCharsets.UTF_8);
        } else if (body != null && !body.isEmpty()) {
            return body;
        }
        return null;
    }

    private Map<String, Collection<String>> parseHeaders() {
        Map<String, Collection<String>> headerMap = new HashMap<>();

        // Add Content-Type if specified
        if (contentType != null && !contentType.isEmpty()) {
            headerMap.put("Content-Type", Collections.singletonList(contentType));
        }

        // Parse headers from -H options
        if (headers != null) {
            for (String header : headers) {
                int colonIndex = header.indexOf(':');
                if (colonIndex > 0) {
                    String name = header.substring(0, colonIndex).trim();
                    String value = header.substring(colonIndex + 1).trim();
                    headerMap.computeIfAbsent(name, k -> new ArrayList<>());
                    ((List<String>) headerMap.get(name)).add(value);
                }
            }
        }

        return headerMap;
    }

    /**
     * Load specification content from a file path or URL.
     */
    private String loadSpecification(String specPath) throws IOException {
        if (specPath.startsWith("http://") || specPath.startsWith("https://")) {
            return loadFromUrl(specPath);
        } else {
            return loadFromFile(specPath);
        }
    }

    private String loadFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }

    private String loadFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        try (InputStream is = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
