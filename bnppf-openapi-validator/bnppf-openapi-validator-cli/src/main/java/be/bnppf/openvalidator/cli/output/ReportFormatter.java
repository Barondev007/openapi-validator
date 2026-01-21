package be.bnppf.openvalidator.cli.output;

import be.bnppf.openvalidator.ValidationReport;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Formats ValidationReport objects for different output formats.
 */
public class ReportFormatter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    private final boolean useColors;

    /**
     * Create a formatter with default settings (colors enabled).
     */
    public ReportFormatter() {
        this(true);
    }

    /**
     * Create a formatter with specified color settings.
     *
     * @param useColors whether to use ANSI colors in text output
     */
    public ReportFormatter(boolean useColors) {
        this.useColors = useColors && !System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Format a validation report in the specified format.
     *
     * @param report the validation report
     * @param format the output format
     * @param context additional context information (e.g., spec path)
     * @return formatted string
     */
    public String format(ValidationReport report, OutputFormat format, String context) {
        switch (format) {
            case JSON:
                return formatJson(report);
            case JUNIT:
                return formatJunit(report, context);
            case TEXT:
            default:
                return formatText(report);
        }
    }

    /**
     * Format as human-readable text.
     */
    public String formatText(ValidationReport report) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\n");
        if (report.isValid()) {
            sb.append(colorize("VALIDATION PASSED", ANSI_GREEN));
        } else {
            sb.append(colorize("VALIDATION FAILED", ANSI_RED));
        }
        sb.append("\n");
        sb.append("─".repeat(50)).append("\n\n");

        // Summary
        sb.append(report.getSummary()).append("\n\n");

        // Messages
        List<ValidationReport.Message> errors = report.getErrors();
        List<ValidationReport.Message> warnings = report.getWarnings();
        List<ValidationReport.Message> infos = report.getInfoMessages();

        if (!errors.isEmpty()) {
            sb.append(colorize("Errors:", ANSI_RED)).append("\n");
            for (ValidationReport.Message msg : errors) {
                sb.append("  ").append(colorize("✗", ANSI_RED)).append(" ");
                appendMessage(sb, msg);
            }
            sb.append("\n");
        }

        if (!warnings.isEmpty()) {
            sb.append(colorize("Warnings:", ANSI_YELLOW)).append("\n");
            for (ValidationReport.Message msg : warnings) {
                sb.append("  ").append(colorize("⚠", ANSI_YELLOW)).append(" ");
                appendMessage(sb, msg);
            }
            sb.append("\n");
        }

        if (!infos.isEmpty()) {
            sb.append(colorize("Info:", ANSI_BLUE)).append("\n");
            for (ValidationReport.Message msg : infos) {
                sb.append("  ").append(colorize("ℹ", ANSI_BLUE)).append(" ");
                appendMessage(sb, msg);
            }
            sb.append("\n");
        }

        if (errors.isEmpty() && warnings.isEmpty() && infos.isEmpty()) {
            sb.append(colorize("No issues found.", ANSI_GREEN)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Format as JSON.
     */
    public String formatJson(ValidationReport report) {
        return report.toJson();
    }

    /**
     * Format as JUnit XML.
     */
    public String formatJunit(ValidationReport report, String context) {
        StringBuilder sb = new StringBuilder();

        String testName = context != null ? context : "OpenAPI Validation";
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneId.systemDefault()));

        List<ValidationReport.Message> errors = report.getErrors();
        List<ValidationReport.Message> warnings = report.getWarnings();

        int failures = errors.size();
        int tests = Math.max(1, errors.size() + warnings.size());

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<testsuite name=\"").append(escapeXml(testName)).append("\" ");
        sb.append("tests=\"").append(tests).append("\" ");
        sb.append("failures=\"").append(failures).append("\" ");
        sb.append("errors=\"0\" ");
        sb.append("skipped=\"0\" ");
        sb.append("timestamp=\"").append(timestamp).append("\">\n");

        if (errors.isEmpty() && warnings.isEmpty()) {
            // Single passing test case
            sb.append("  <testcase name=\"validation\" classname=\"").append(escapeXml(testName)).append("\">\n");
            sb.append("  </testcase>\n");
        } else {
            // Create a test case for each error/warning
            int caseNum = 1;
            for (ValidationReport.Message msg : errors) {
                sb.append("  <testcase name=\"").append(escapeXml(msg.getKey() != null ? msg.getKey() : "error_" + caseNum))
                        .append("\" classname=\"").append(escapeXml(testName)).append("\">\n");
                sb.append("    <failure message=\"").append(escapeXml(msg.getMessage())).append("\" type=\"ValidationError\">\n");
                sb.append("      ").append(escapeXml(formatMessageDetail(msg))).append("\n");
                sb.append("    </failure>\n");
                sb.append("  </testcase>\n");
                caseNum++;
            }

            for (ValidationReport.Message msg : warnings) {
                sb.append("  <testcase name=\"").append(escapeXml(msg.getKey() != null ? msg.getKey() : "warning_" + caseNum))
                        .append("\" classname=\"").append(escapeXml(testName)).append("\">\n");
                sb.append("    <system-out><![CDATA[WARNING: ").append(formatMessageDetail(msg)).append("]]></system-out>\n");
                sb.append("  </testcase>\n");
                caseNum++;
            }
        }

        sb.append("</testsuite>\n");

        return sb.toString();
    }

    private void appendMessage(StringBuilder sb, ValidationReport.Message msg) {
        if (msg.getContext() != null && !msg.getContext().isEmpty()) {
            sb.append("[").append(msg.getContext()).append("] ");
        }
        sb.append(msg.getMessage());
        if (msg.getKey() != null && !msg.getKey().isEmpty()) {
            sb.append(" (").append(msg.getKey()).append(")");
        }
        if (!msg.getAdditionalInfo().isEmpty()) {
            sb.append("\n      Additional info: ").append(String.join("; ", msg.getAdditionalInfo()));
        }
        sb.append("\n");
    }

    private String formatMessageDetail(ValidationReport.Message msg) {
        StringBuilder sb = new StringBuilder();
        if (msg.getContext() != null && !msg.getContext().isEmpty()) {
            sb.append("Context: ").append(msg.getContext()).append("\n");
        }
        sb.append("Message: ").append(msg.getMessage());
        if (!msg.getAdditionalInfo().isEmpty()) {
            sb.append("\nAdditional info: ").append(String.join("; ", msg.getAdditionalInfo()));
        }
        return sb.toString();
    }

    private String colorize(String text, String color) {
        if (useColors) {
            return color + text + ANSI_RESET;
        }
        return text;
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Format a simple result message.
     *
     * @param success whether validation succeeded
     * @param message the message to display
     * @return formatted string
     */
    public String formatResult(boolean success, String message) {
        if (success) {
            return colorize("✓ " + message, ANSI_GREEN);
        } else {
            return colorize("✗ " + message, ANSI_RED);
        }
    }

    /**
     * Format an error message.
     *
     * @param message the error message
     * @return formatted string
     */
    public String formatError(String message) {
        return colorize("Error: " + message, ANSI_RED);
    }

    /**
     * Format a warning message.
     *
     * @param message the warning message
     * @return formatted string
     */
    public String formatWarning(String message) {
        return colorize("Warning: " + message, ANSI_YELLOW);
    }

    /**
     * Format an info message.
     *
     * @param message the info message
     * @return formatted string
     */
    public String formatInfo(String message) {
        return colorize("Info: " + message, ANSI_BLUE);
    }
}
