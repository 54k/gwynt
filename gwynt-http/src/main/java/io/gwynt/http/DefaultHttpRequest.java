package io.gwynt.http;

import java.util.Map;

final class DefaultHttpRequest implements HttpRequest {

    private HttpMethod method;
    private String uri;
    private HttpVersion version;
    private Map<String, String> headers;

    DefaultHttpRequest(HttpMethod method, String uri, HttpVersion version) {
        this.method = method;
        this.uri = uri;
        this.version = version;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public HttpVersion getVersion() {
        return version;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }
}
