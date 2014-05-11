package io.gwynt.websocket.protocol;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.IoSession;
import io.gwynt.core.pipeline.IoHandlerContext;
import io.gwynt.websocket.DefaultWebSocketSession;
import io.gwynt.websocket.WebSocketIoHandler;
import io.gwynt.websocket.exception.HandshakeException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class WebSocketHandshakeCodec extends AbstractIoHandler<byte[], Handshake> {

    public static final String HANDSHAKE_SERVER_OK_STATUS_LINE = "HTTP/1.1 101 Switching protocols";
    public static final String HANDSHAKE_SERVER_FORBIDDEN_STATUS_LINE = "HTTP/1.1 403 Forbidden";
    public static final String HEADER_WEB_SOCKET_ACCEPT = "Sec-WebSocket-Accept";
    public static final String HEADER_WEB_SOCKET_KEY = "Sec-WebSocket-Key";
    public static final String HEADER_CONNECTION_KEY = "Connection";
    public static final String HEADER_CONNECTION_VALUE = "Upgrade";
    public static final String HEADER_UPGRADE_KEY = "Upgrade";
    public static final String HEADER_UPGRADE_VALUE = "websocket";
    public static final String RFC_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    public static final String CRLF = "\r\n";
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeCodec.class);
    private WebSocketIoHandler eventHandler;

    public WebSocketHandshakeCodec(WebSocketIoHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    protected static Handshake parseClientHandshake(byte[] clientRequest) {
        Handshake handshake = new Handshake();

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(clientRequest);
            BufferedReader reader = new BufferedReader(new StringReader(new String(clientRequest)));

            String statusLine = reader.readLine();
            handshake.setStatusLine(statusLine);
            String headerLine;
            while ((headerLine = reader.readLine()) != null) {
                if (headerLine.isEmpty()) {
                    continue;
                }
                String[] header = headerLine.split(":", 2);
                if (header.length != 2) {
                    throw new HandshakeException(headerLine + " is not a header");
                }
                handshake.addHeader(header[0].trim(), header[1].trim());
            }
        } catch (IOException e) {
            throw new HandshakeException(e);
        }

        validateClientHandshake(handshake);

        return handshake;
    }

    protected static void validateClientHandshake(Handshake handshake) {
        Map<String, String> headers = handshake.getHeaders();
        String upgrade = headers.get(HEADER_UPGRADE_KEY);
        String webSocketKey = headers.get(HEADER_WEB_SOCKET_KEY);
        if (!(HEADER_UPGRADE_VALUE.equals(upgrade) && webSocketKey != null)) {
            throw new HandshakeException("Websocket headers are not recognized");
        }
    }

    protected static Handshake buildServerHandshake(Handshake clientHandshake) {
        Handshake handshake = new Handshake();
        handshake.setStatusLine(HANDSHAKE_SERVER_OK_STATUS_LINE);

        handshake.addHeader(HEADER_CONNECTION_KEY, HEADER_CONNECTION_VALUE);
        handshake.addHeader(HEADER_UPGRADE_KEY, HEADER_UPGRADE_VALUE);

        String key = clientHandshake.getHeaderValue(HEADER_WEB_SOCKET_KEY);
        handshake.addHeader(HEADER_WEB_SOCKET_ACCEPT, encodeClientKey(key));

        return handshake;
    }

    protected static String encodeClientKey(String key) {
        return Base64.encodeBase64String(DigestUtils.sha1((key + RFC_GUID).getBytes()));
    }

    @Override
    public void onMessageSent(IoHandlerContext context, Handshake message) {
        context.fireMessageSent(StringUtils.getBytesUtf8(message.toString()));
    }

    @Override
    public void onMessageReceived(IoHandlerContext context, byte[] message) {
        IoSession ioSession = context.getIoSession();
        final DefaultWebSocketSession webSocketConnection = (DefaultWebSocketSession) ioSession.attachment();

        try {
            Handshake clientHandshake = parseClientHandshake(message);
            Handshake serverHandshake = buildServerHandshake(clientHandshake);
            webSocketConnection.setHandshake(clientHandshake);
            onMessageSent(context, serverHandshake);

            webSocketConnection.completeHandshake();

            context.getIoSession().getPipeline().remove(this);
            context.getIoSession().getPipeline().addLast(new WebSocketFrameCodec(webSocketConnection) {
                @Override
                protected boolean isServer() {
                    return true;
                }
            });

            eventHandler.onOpen(webSocketConnection);
        } catch (HandshakeException e) {
            Handshake handshake = new Handshake();
            handshake.setStatusLine(HANDSHAKE_SERVER_FORBIDDEN_STATUS_LINE);
            onMessageSent(context, handshake);
            context.fireClosing();
            logger.warn("Invalid handshake data: {}, closing session {}", e.getMessage(), ioSession);
        }
    }
}
