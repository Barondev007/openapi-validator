package be.bnppf.openvalidator.cli;

import be.bnppf.openvalidator.cli.commands.ValidateRequestCommand;
import be.bnppf.openvalidator.cli.commands.ValidateResponseCommand;
import be.bnppf.openvalidator.cli.commands.ValidateSpecCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command-line interface for OpenAPI validation.
 *
 * <p>Usage examples:
 *
 * <p>1. Validate a specification file:
 * <pre>
 * java -jar bnppf-openapi-validator-cli.jar validate-spec --spec /path/to/openapi.yaml
 * java -jar bnppf-openapi-validator-cli.jar validate-spec --spec /path/to/openapi.yaml --level strict
 * </pre>
 *
 * <p>2. Validate a request against a spec:
 * <pre>
 * java -jar bnppf-openapi-validator-cli.jar validate-request \
 *     --spec /path/to/openapi.yaml \
 *     --method POST \
 *     --path /pets \
 *     --body '{"name": "Fluffy", "tag": "cat"}' \
 *     --content-type application/json
 * </pre>
 *
 * <p>3. Validate a request from file:
 * <pre>
 * java -jar bnppf-openapi-validator-cli.jar validate-request \
 *     --spec /path/to/openapi.yaml \
 *     --method POST \
 *     --path /pets \
 *     --body-file /path/to/request.json \
 *     --header "Authorization: Bearer token123" \
 *     --header "X-Custom-Header: value"
 * </pre>
 *
 * <p>4. Validate a response:
 * <pre>
 * java -jar bnppf-openapi-validator-cli.jar validate-response \
 *     --spec /path/to/openapi.yaml \
 *     --method GET \
 *     --path /pets/123 \
 *     --status 200 \
 *     --body '{"id": 123, "name": "Fluffy"}' \
 *     --content-type application/json
 * </pre>
 *
 * <p>5. Interactive mode (read from stdin):
 * <pre>
 * echo '{"name": "Fluffy"}' | java -jar bnppf-openapi-validator-cli.jar validate-request \
 *     --spec /path/to/openapi.yaml \
 *     --method POST \
 *     --path /pets \
 *     --stdin
 * </pre>
 *
 * <p>Output formats:
 * <ul>
 *   <li>--output text (default, human-readable)</li>
 *   <li>--output json (machine-readable)</li>
 *   <li>--output junit (JUnit XML format for CI/CD)</li>
 * </ul>
 */
@Command(
        name = "bnppf-openapi-validator",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "OpenAPI/Swagger specification and request/response validator",
        subcommands = {
                ValidateSpecCommand.class,
                ValidateRequestCommand.class,
                ValidateResponseCommand.class
        }
)
public class OpenAPIValidatorCLI implements Runnable {

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
    )
    boolean verbose;

    @Option(
            names = {"--debug"},
            description = "Enable debug mode"
    )
    boolean debug;

    /**
     * Main entry point for the CLI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new OpenAPIValidatorCLI())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // When called without subcommand, show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Check if verbose mode is enabled.
     *
     * @return true if verbose mode is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Check if debug mode is enabled.
     *
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }
}
