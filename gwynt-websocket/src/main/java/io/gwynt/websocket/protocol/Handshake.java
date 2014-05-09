package io.gwynt.websocket.protocol;

import java.util.HashMap;
import java.util.Map;

public class Handshake {

    private String statusLine;
    private Map<String, String> headers = new HashMap<>();

    public String getStatusLine() {
        return statusLine;
    }

    public void setStatusLine(String statusLine) {
        this.statusLine = statusLine;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeaderValue(String name) {
        return headers.get(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(statusLine).append(WebSocketHandshakeCodec.CRLF);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(WebSocketHandshakeCodec.CRLF);
        }
        sb.append(WebSocketHandshakeCodec.CRLF);
        return sb.toString();
    }
}
