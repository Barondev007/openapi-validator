package be.bnppf.openvalidator;

/**
 * Defines the strictness level for validation.
 *
 * <p><b>LIGHT:</b>
 * <ul>
 *   <li>Only validates required fields</li>
 *   <li>Ignores additional properties</li>
 *   <li>Ignores format validation</li>
 *   <li>Useful for development/testing</li>
 * </ul>
 *
 * <p><b>LENIENT:</b>
 * <ul>
 *   <li>Validates required fields</li>
 *   <li>Validates types</li>
 *   <li>Allows additional properties (additionalProperties: true by default)</li>
 *   <li>Validates string formats loosely</li>
 *   <li>Default mode for most use cases</li>
 * </ul>
 *
 * <p><b>STRICT:</b>
 * <ul>
 *   <li>Full OpenAPI specification compliance</li>
 *   <li>No additional properties allowed (unless explicitly specified)</li>
 *   <li>Strict format validation (dates, emails, uris, etc.)</li>
 *   <li>Validates all constraints (min, max, pattern, enum, etc.)</li>
 *   <li>Recommended for production</li>
 * </ul>
 */
public enum ValidationLevel {

    /**
     * Light validation - only validates required fields.
     */
    LIGHT("light"),

    /**
     * Lenient validation - validates types and required fields, allows additional properties.
     */
    LENIENT("lenient"),

    /**
     * Strict validation - full OpenAPI specification compliance.
     */
    STRICT("strict");

    private final String value;

    ValidationLevel(String value) {
        this.value = value;
    }

    /**
     * Get the string value of this validation level.
     *
     * @return the string representation
     */
    public String getValue() {
        return value;
    }

    /**
     * Parse a string value to a ValidationLevel.
     * Case-insensitive matching. Returns LENIENT if value is null or unrecognized.
     *
     * @param value the string value to parse
     * @return the corresponding ValidationLevel, or LENIENT as default
     */
    public static ValidationLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LENIENT;
        }

        String normalized = value.trim().toLowerCase();
        for (ValidationLevel level : values()) {
            if (level.value.equals(normalized) || level.name().toLowerCase().equals(normalized)) {
                return level;
            }
        }

        return LENIENT;
    }

    /**
     * Check if this level is more strict than another level.
     *
     * @param other the level to compare against
     * @return true if this level is more strict
     */
    public boolean isMoreStrictThan(ValidationLevel other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * Check if this level is less strict than another level.
     *
     * @param other the level to compare against
     * @return true if this level is less strict
     */
    public boolean isLessStrictThan(ValidationLevel other) {
        return this.ordinal() < other.ordinal();
    }

    @Override
    public String toString() {
        return value;
    }
}
