package io.gwynt.http;

import java.util.Map;

public interface HttpObject {

    HttpVersion getVersion();

    Map<String, String> getHeaders();
}
