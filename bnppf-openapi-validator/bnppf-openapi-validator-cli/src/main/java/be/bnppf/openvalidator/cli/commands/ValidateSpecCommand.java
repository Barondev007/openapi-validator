package be.bnppf.openvalidator.cli.commands;

import be.bnppf.openvalidator.BnppfOpenAPIValidator;
import be.bnppf.openvalidator.ValidationLevel;
import be.bnppf.openvalidator.ValidationReport;
import be.bnppf.openvalidator.cli.output.OutputFormat;
import be.bnppf.openvalidator.cli.output.ReportFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
            // Load the specification
            BnppfOpenAPIValidator validator;
            if (specPath.startsWith("http://") || specPath.startsWith("https://")) {
                validator = BnppfOpenAPIValidator.getInstanceFromUrl(specPath, validationLevel);
            } else {
                validator = BnppfOpenAPIValidator.getInstanceFromFile(specPath, validationLevel);
            }

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
        } catch (Exception e) {
            System.err.println(formatter.formatError("Unexpected error: " + e.getMessage()));
            if (isVerbose()) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private boolean isVerbose() {
        // Check if parent command has verbose flag set
        // This is a simplified check - in a real implementation, you'd access the parent's verbose field
        return false;
    }
}
