package be.bnppf.openvalidator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Contains the results of a validation operation.
 * Provides methods to check for errors/warnings and retrieve validation messages.
 */
public class ValidationReport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Severity level for validation messages.
     */
    public enum Level {
        /**
         * Error - validation failed.
         */
        ERROR,

        /**
         * Warning - potential issue but not blocking.
         */
        WARN,

        /**
         * Informational message.
         */
        INFO,

        /**
         * Message was ignored due to validation level configuration.
         */
        IGNORE
    }

    /**
     * Represents a single validation message.
     */
    public static class Message {
        private Level level;
        private String key;
        private String message;
        private String context;
        private List<String> additionalInfo;

        /**
         * Default constructor.
         */
        public Message() {
            this.additionalInfo = new ArrayList<>();
        }

        /**
         * Full constructor.
         *
         * @param level          severity level
         * @param key            unique message key
         * @param message        human-readable message
         * @param context        path or location context
         * @param additionalInfo additional information
         */
        public Message(Level level, String key, String message, String context, List<String> additionalInfo) {
            this.level = level;
            this.key = key;
            this.message = message;
            this.context = context;
            this.additionalInfo = additionalInfo != null ? new ArrayList<>(additionalInfo) : new ArrayList<>();
        }

        // Getters and setters

        public Level getLevel() {
            return level;
        }

        public Message setLevel(Level level) {
            this.level = level;
            return this;
        }

        public String getKey() {
            return key;
        }

        public Message setKey(String key) {
            this.key = key;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Message setMessage(String message) {
            this.message = message;
            return this;
        }

        public String getContext() {
            return context;
        }

        public Message setContext(String context) {
            this.context = context;
            return this;
        }

        public List<String> getAdditionalInfo() {
            return Collections.unmodifiableList(additionalInfo);
        }

        public Message setAdditionalInfo(List<String> additionalInfo) {
            this.additionalInfo = additionalInfo != null ? new ArrayList<>(additionalInfo) : new ArrayList<>();
            return this;
        }

        public Message addAdditionalInfo(String info) {
            this.additionalInfo.add(info);
            return this;
        }

        /**
         * Builder pattern static method.
         *
         * @return a new Message builder
         */
        public static MessageBuilder builder() {
            return new MessageBuilder();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(level).append("]");
            if (context != null && !context.isEmpty()) {
                sb.append(" ").append(context).append(":");
            }
            sb.append(" ").append(message);
            if (key != null && !key.isEmpty()) {
                sb.append(" (").append(key).append(")");
            }
            if (!additionalInfo.isEmpty()) {
                sb.append(" - ").append(String.join("; ", additionalInfo));
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message1 = (Message) o;
            return level == message1.level &&
                    Objects.equals(key, message1.key) &&
                    Objects.equals(message, message1.message) &&
                    Objects.equals(context, message1.context);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, key, message, context);
        }

        /**
         * Builder for Message objects.
         */
        public static class MessageBuilder {
            private Level level;
            private String key;
            private String message;
            private String context;
            private List<String> additionalInfo = new ArrayList<>();

            public MessageBuilder level(Level level) {
                this.level = level;
                return this;
            }

            public MessageBuilder key(String key) {
                this.key = key;
                return this;
            }

            public MessageBuilder message(String message) {
                this.message = message;
                return this;
            }

            public MessageBuilder context(String context) {
                this.context = context;
                return this;
            }

            public MessageBuilder additionalInfo(List<String> additionalInfo) {
                this.additionalInfo = additionalInfo != null ? new ArrayList<>(additionalInfo) : new ArrayList<>();
                return this;
            }

            public MessageBuilder addAdditionalInfo(String info) {
                this.additionalInfo.add(info);
                return this;
            }

            public Message build() {
                return new Message(level, key, message, context, additionalInfo);
            }
        }
    }

    private final List<Message> messages;
    private boolean isValid;

    /**
     * Creates an empty validation report.
     */
    public ValidationReport() {
        this.messages = new ArrayList<>();
        this.isValid = true;
    }

    /**
     * Creates a validation report with the given messages.
     *
     * @param messages the validation messages
     */
    public ValidationReport(List<Message> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
        this.isValid = !hasErrors();
    }

    /**
     * Check if the validation found any errors.
     *
     * @return true if there are error-level messages
     */
    public boolean hasErrors() {
        return messages.stream().anyMatch(m -> m.getLevel() == Level.ERROR);
    }

    /**
     * Check if the validation found any warnings.
     *
     * @return true if there are warning-level messages
     */
    public boolean hasWarnings() {
        return messages.stream().anyMatch(m -> m.getLevel() == Level.WARN);
    }

    /**
     * Check if the validation is valid (no errors).
     *
     * @return true if there are no errors
     */
    public boolean isValid() {
        return !hasErrors();
    }

    /**
     * Get all validation messages.
     *
     * @return unmodifiable list of all messages
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Get only error messages.
     *
     * @return list of error-level messages
     */
    public List<Message> getErrors() {
        return messages.stream()
                .filter(m -> m.getLevel() == Level.ERROR)
                .collect(Collectors.toList());
    }

    /**
     * Get only warning messages.
     *
     * @return list of warning-level messages
     */
    public List<Message> getWarnings() {
        return messages.stream()
                .filter(m -> m.getLevel() == Level.WARN)
                .collect(Collectors.toList());
    }

    /**
     * Get only info messages.
     *
     * @return list of info-level messages
     */
    public List<Message> getInfoMessages() {
        return messages.stream()
                .filter(m -> m.getLevel() == Level.INFO)
                .collect(Collectors.toList());
    }

    /**
     * Get messages filtered by level.
     *
     * @param level the level to filter by
     * @return list of messages at the specified level
     */
    public List<Message> getMessagesByLevel(Level level) {
        return messages.stream()
                .filter(m -> m.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Get the number of messages at each level.
     *
     * @return a summary string
     */
    public String getSummary() {
        long errors = messages.stream().filter(m -> m.getLevel() == Level.ERROR).count();
        long warnings = messages.stream().filter(m -> m.getLevel() == Level.WARN).count();
        long infos = messages.stream().filter(m -> m.getLevel() == Level.INFO).count();

        return String.format("Validation result: %d error(s), %d warning(s), %d info message(s)",
                errors, warnings, infos);
    }

    /**
     * Get a formatted string of all messages.
     *
     * @return newline-separated string of all messages
     */
    public String getMessagesAsString() {
        if (messages.isEmpty()) {
            return "No validation messages";
        }
        return messages.stream()
                .map(Message::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get error messages as a formatted string.
     *
     * @return newline-separated string of error messages
     */
    public String getErrorsAsString() {
        List<Message> errors = getErrors();
        if (errors.isEmpty()) {
            return "No errors";
        }
        return errors.stream()
                .map(Message::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Get messages as JSON string.
     *
     * @return JSON representation of the validation report
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(new JsonReport(this));
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize report: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Add a message to this report.
     *
     * @param message the message to add
     * @return this report for chaining
     */
    public ValidationReport addMessage(Message message) {
        if (message != null) {
            this.messages.add(message);
            if (message.getLevel() == Level.ERROR) {
                this.isValid = false;
            }
        }
        return this;
    }

    /**
     * Add an error message to this report.
     *
     * @param key     the message key
     * @param message the message text
     * @param context the context
     * @return this report for chaining
     */
    public ValidationReport addError(String key, String message, String context) {
        return addMessage(Message.builder()
                .level(Level.ERROR)
                .key(key)
                .message(message)
                .context(context)
                .build());
    }

    /**
     * Add a warning message to this report.
     *
     * @param key     the message key
     * @param message the message text
     * @param context the context
     * @return this report for chaining
     */
    public ValidationReport addWarning(String key, String message, String context) {
        return addMessage(Message.builder()
                .level(Level.WARN)
                .key(key)
                .message(message)
                .context(context)
                .build());
    }

    /**
     * Add an info message to this report.
     *
     * @param key     the message key
     * @param message the message text
     * @param context the context
     * @return this report for chaining
     */
    public ValidationReport addInfo(String key, String message, String context) {
        return addMessage(Message.builder()
                .level(Level.INFO)
                .key(key)
                .message(message)
                .context(context)
                .build());
    }

    /**
     * Create an empty (valid) report.
     *
     * @return a new empty ValidationReport
     */
    public static ValidationReport empty() {
        return new ValidationReport();
    }

    /**
     * Create a report with a single error.
     *
     * @param key     the error key
     * @param message the error message
     * @param context the error context
     * @return a new ValidationReport with one error
     */
    public static ValidationReport withError(String key, String message, String context) {
        ValidationReport report = new ValidationReport();
        report.addError(key, message, context);
        return report;
    }

    /**
     * Merge multiple validation reports.
     *
     * @param reports the reports to merge
     * @return a new ValidationReport containing all messages from the given reports
     */
    public static ValidationReport merge(ValidationReport... reports) {
        ValidationReport merged = new ValidationReport();
        if (reports != null) {
            for (ValidationReport report : reports) {
                if (report != null) {
                    merged.messages.addAll(report.messages);
                }
            }
            merged.isValid = !merged.hasErrors();
        }
        return merged;
    }

    /**
     * Merge this report with another.
     *
     * @param other the other report
     * @return a new merged ValidationReport
     */
    public ValidationReport merge(ValidationReport other) {
        return merge(this, other);
    }

    @Override
    public String toString() {
        return getSummary() + "\n" + getMessagesAsString();
    }

    /**
     * Internal class for JSON serialization.
     */
    private static class JsonReport {
        public final boolean valid;
        public final int errorCount;
        public final int warningCount;
        public final int infoCount;
        public final List<Message> messages;

        JsonReport(ValidationReport report) {
            this.valid = report.isValid();
            this.errorCount = report.getErrors().size();
            this.warningCount = report.getWarnings().size();
            this.infoCount = report.getInfoMessages().size();
            this.messages = report.getMessages();
        }
    }
}
