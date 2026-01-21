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
import java.util.concurrent.Callable;

/**
 * Command to validate an OpenAPI/Swagger specification file.
 */
@Command(
        name = "validate-spec",
        description = "Validate an OpenAPI/Swagger specification file",
        mixinStandardHelpOptions = true
)
public class ValidateSpecCommand implements Callable<Integer> {

    @Option(
            names = {"-s", "--spec"},
            required = true,
            description = "Path or URL to the OpenAPI specification"
    )
    private String specPath;

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
            names = {"--fail-on-warn"},
            description = "Exit with error code if warnings are found"
    )
    private boolean failOnWarn;

    @Option(
            names = {"--no-color"},
            description = "Disable colored output"
    )
    private boolean noColor;

    @CommandLine.ParentCommand
    private Object parent;

    @Override
    public Integer call() {
        ReportFormatter formatter = new ReportFormatter(!noColor);
        OutputFormat format = OutputFormat.fromString(outputFormat);
        ValidationLevel validationLevel = ValidationLevel.fromString(level);

        try {
            // Load the specification content
            String specContent = loadSpecification(specPath);

            // Create validator from content string
            BnppfOpenAPIValidator validator = BnppfOpenAPIValidator.getInstance(specContent, validationLevel);

            // Validate the specification
            ValidationReport report = validator.validateSpecification();

            // Output the report
            System.out.println(formatter.format(report, format, specPath));

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
            System.err.println(formatter.formatError("Failed to load specification: " + e.getMessage()));
            return 1;
        } catch (Exception e) {
            System.err.println(formatter.formatError("Unexpected error: " + e.getMessage()));
            if (isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    /**
     * Load specification content from a file path or URL.
     *
     * @param path the file path or URL
     * @return the specification content as a string
     * @throws IOException if the specification cannot be loaded
     */
    private String loadSpecification(String path) throws IOException {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return loadFromUrl(path);
        } else {
            return loadFromFile(path);
        }
    }

    /**
     * Load specification content from a file.
     */
    private String loadFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }

    /**
     * Load specification content from a URL.
     */
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

    private boolean isVerbose() {
        // Check if parent command has verbose flag set
        return false;
    }
}
