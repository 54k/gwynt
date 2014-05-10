package io.gwynt.example;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.TcpEndpoint;
import io.gwynt.core.pipeline.IoHandlerContext;

import java.util.Date;

public class Main {

    public static void main(String[] args) throws Exception {

        new TcpEndpoint().addHandler(new AbstractIoHandler<byte[], String>() {

            @Override
            public void onMessageReceived(IoHandlerContext context, byte[] message) {
                context.fireMessageReceived(new String(message));
            }

            @Override
            public void onMessageSent(IoHandlerContext context, String message) {
                context.fireMessageSent(message.getBytes());
            }

        }).addHandler(new AbstractIoHandler<String, Object>() {

            @Override
            public void onMessageReceived(IoHandlerContext context, String message) {
                context.fireMessageSent("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
                context.fireMessageSent(new Date().toString() + "\r\n");
                context.fireClosing();
            }

            @Override
            public void onExceptionCaught(IoHandlerContext context, Throwable e) {
                e.printStackTrace();
            }

        }).bind(3000);
    }
}
