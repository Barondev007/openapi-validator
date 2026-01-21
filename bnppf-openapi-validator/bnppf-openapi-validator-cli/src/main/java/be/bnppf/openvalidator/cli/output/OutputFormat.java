package be.bnppf.openvalidator.cli.output;

/**
 * Supported output formats for the CLI.
 */
public enum OutputFormat {
    /**
     * Human-readable text output (default).
     */
    TEXT("text"),

    /**
     * Machine-readable JSON output.
     */
    JSON("json"),

    /**
     * JUnit XML format for CI/CD integration.
     */
    JUNIT("junit");

    private final String value;

    OutputFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse a string value to an OutputFormat.
     * Case-insensitive matching. Returns TEXT if value is null or unrecognized.
     *
     * @param value the string value to parse
     * @return the corresponding OutputFormat, or TEXT as default
     */
    public static OutputFormat fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TEXT;
        }

        String normalized = value.trim().toLowerCase();
        for (OutputFormat format : values()) {
            if (format.value.equals(normalized) || format.name().toLowerCase().equals(normalized)) {
                return format;
            }
        }

        return TEXT;
    }

    @Override
    public String toString() {
        return value;
    }
}
