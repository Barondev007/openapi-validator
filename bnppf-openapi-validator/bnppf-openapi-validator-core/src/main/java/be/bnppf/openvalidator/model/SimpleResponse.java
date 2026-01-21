package be.bnppf.openvalidator.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple representation of an HTTP response for validation purposes.
 * This class provides a framework-agnostic way to represent response data.
 */
public class SimpleResponse {

    private final int statusCode;
    private final String body;
    private final Map<String, Collection<String>> headers;
    private final String contentType;

    private SimpleResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.body = builder.body;
        this.headers = builder.headers != null ?
                Collections.unmodifiableMap(new HashMap<>(builder.headers)) :
                Collections.emptyMap();
        this.contentType = builder.contentType;
    }

    /**
     * Get the HTTP status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the response body.
     *
     * @return the response body, or null if not present
     */
    public String getBody() {
        return body;
    }

    /**
     * Get the HTTP headers.
     *
     * @return unmodifiable map of headers
     */
    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }

    /**
     * Get the Content-Type header value.
     *
     * @return the content type, or null if not set
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Check if this response has a body.
     *
     * @return true if body is present and non-empty
     */
    public boolean hasBody() {
        return body != null && !body.isEmpty();
    }

    /**
     * Check if this is a success status code (2xx).
     *
     * @return true if status code is in 200-299 range
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Check if this is a client error status code (4xx).
     *
     * @return true if status code is in 400-499 range
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Check if this is a server error status code (5xx).
     *
     * @return true if status code is in 500-599 range
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Create a new builder.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "SimpleResponse{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", hasBody=" + hasBody() +
                ", headers=" + headers.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleResponse that = (SimpleResponse) o;
        return statusCode == that.statusCode &&
                Objects.equals(body, that.body) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, body, headers, contentType);
    }

    /**
     * Builder for SimpleResponse.
     */
    public static class Builder {
        private int statusCode = 200;
        private String body;
        private Map<String, Collection<String>> headers = new HashMap<>();
        private String contentType;

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder headers(Map<String, Collection<String>> headers) {
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            return this;
        }

        public Builder addHeader(String name, Collection<String> values) {
            this.headers.put(name, values);
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers.put(name, Collections.singletonList(value));
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            if (contentType != null) {
                this.headers.put("Content-Type", Collections.singletonList(contentType));
            }
            return this;
        }

        public SimpleResponse build() {
            return new SimpleResponse(this);
        }
    }
}
