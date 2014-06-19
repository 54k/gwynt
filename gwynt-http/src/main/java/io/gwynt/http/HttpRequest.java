package io.gwynt.http;

public interface HttpRequest extends HttpObject {

    HttpMethod getMethod();

    String getUri();

}
