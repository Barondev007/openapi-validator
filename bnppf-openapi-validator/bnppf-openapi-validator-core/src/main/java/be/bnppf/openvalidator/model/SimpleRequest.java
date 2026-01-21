package be.bnppf.openvalidator.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple representation of an HTTP request for validation purposes.
 * This class provides a framework-agnostic way to represent request data.
 */
public class SimpleRequest {

    private final String method;
    private final String path;
    private final String body;
    private final Map<String, Collection<String>> queryParams;
    private final Map<String, Collection<String>> headers;
    private final String contentType;

    private SimpleRequest(Builder builder) {
        this.method = builder.method;
        this.path = builder.path;
        this.body = builder.body;
        this.queryParams = builder.queryParams != null ?
                Collections.unmodifiableMap(new HashMap<>(builder.queryParams)) :
                Collections.emptyMap();
        this.headers = builder.headers != null ?
                Collections.unmodifiableMap(new HashMap<>(builder.headers)) :
                Collections.emptyMap();
        this.contentType = builder.contentType;
    }

    /**
     * Get the HTTP method (GET, POST, PUT, DELETE, etc.).
     *
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get the request path.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the request body.
     *
     * @return the request body, or null if not present
     */
    public String getBody() {
        return body;
    }

    /**
     * Get the query parameters.
     *
     * @return unmodifiable map of query parameters
     */
    public Map<String, Collection<String>> getQueryParams() {
        return queryParams;
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
     * Check if this request has a body.
     *
     * @return true if body is present and non-empty
     */
    public boolean hasBody() {
        return body != null && !body.isEmpty();
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
        return "SimpleRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", contentType='" + contentType + '\'' +
                ", hasBody=" + hasBody() +
                ", queryParams=" + queryParams.size() +
                ", headers=" + headers.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleRequest that = (SimpleRequest) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(path, that.path) &&
                Objects.equals(body, that.body) &&
                Objects.equals(queryParams, that.queryParams) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, body, queryParams, headers, contentType);
    }

    /**
     * Builder for SimpleRequest.
     */
    public static class Builder {
        private String method;
        private String path;
        private String body;
        private Map<String, Collection<String>> queryParams = new HashMap<>();
        private Map<String, Collection<String>> headers = new HashMap<>();
        private String contentType;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder queryParams(Map<String, Collection<String>> queryParams) {
            this.queryParams = queryParams != null ? new HashMap<>(queryParams) : new HashMap<>();
            return this;
        }

        public Builder addQueryParam(String name, Collection<String> values) {
            this.queryParams.put(name, values);
            return this;
        }

        public Builder addQueryParam(String name, String value) {
            this.queryParams.put(name, Collections.singletonList(value));
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

        public SimpleRequest build() {
            if (method == null || method.isEmpty()) {
                throw new IllegalArgumentException("HTTP method is required");
            }
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Request path is required");
            }
            return new SimpleRequest(this);
        }
    }
}
