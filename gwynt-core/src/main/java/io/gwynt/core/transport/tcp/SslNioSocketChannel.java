package io.gwynt.core.transport.tcp;

import io.gwynt.core.transport.AbstractSelectableChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

@Deprecated
public class SslNioSocketChannel extends AbstractSelectableChannel<SocketChannel> {

    private SSLEngine engine;
    private SSLSession session;
    private SSLEngineResult result;

    private AtomicBoolean handshakeComplete = new AtomicBoolean(false);

    private ByteBuffer sslReadBuffer;
    private ByteBuffer sslWriteBuffer;

    private ByteBuffer peerReadBuffer;
    private ByteBuffer peerWriteBuffer;

    public SslNioSocketChannel(SocketChannel channel, SSLEngine engine) {
        super(channel);
        this.engine = engine;
        this.session = engine.getSession();
        prepareForHandshake();
    }

    private void prepareForHandshake() {
        sslReadBuffer = ByteBuffer.allocateDirect(session.getApplicationBufferSize());
        sslWriteBuffer = ByteBuffer.allocateDirect(session.getApplicationBufferSize());

        peerReadBuffer = ByteBuffer.allocateDirect(session.getPacketBufferSize());
        peerWriteBuffer = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        try {
            engine.beginHandshake();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

        result = new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW, engine.getHandshakeStatus(), 0, 0);
    }

    private void processHandshakeSequence() throws IOException {
        while (!handshakeComplete.get()) {
            if (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) {
                int read = channel.read(peerReadBuffer);

                if (read == 0) {
                    close();
                    break;
                }

                if (read == -1) {
                    close();
                }

                peerReadBuffer.flip();

                do {
                    result = engine.unwrap(peerReadBuffer, sslReadBuffer);
                }
                while (result.getStatus() == SSLEngineResult.Status.OK && result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP);
            } else if (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
                result = engine.wrap(sslWriteBuffer, peerWriteBuffer);
                peerWriteBuffer.flip();
                channel.write(peerWriteBuffer);
            } else if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                processDelegatedTask();
                result = new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW, engine.getHandshakeStatus(), 0, 0);
            } else if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                handshakeComplete.set(true);
            }
            clearBuffers();
        }
    }

    private void clearBuffers() {
        peerReadBuffer.clear();
        peerWriteBuffer.clear();
        sslReadBuffer.clear();
        sslWriteBuffer.clear();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!handshakeComplete.get()) {
            processHandshakeSequence();
            return 0;
        }

        if (peerReadBuffer.hasRemaining()) {
            channel.read(peerReadBuffer);
            peerReadBuffer.flip();
            engine.unwrap(peerReadBuffer, sslReadBuffer);
            sslReadBuffer.flip();
        }

        int result = 0;
        while (dst.hasRemaining() && sslReadBuffer.hasRemaining()) {
            dst.put(sslReadBuffer.get());
            result++;
        }

        if (!sslReadBuffer.hasRemaining()) {
            clearBuffers();
        }

        return result;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!handshakeComplete.get()) {
            processHandshakeSequence();
            return 0;
        }

        while (peerWriteBuffer.hasRemaining() && src.hasRemaining()) {
            int bytesProduced = engine.wrap(src, peerWriteBuffer).bytesProduced();
            if (bytesProduced == 0) {
                break;
            }
        }
        peerWriteBuffer.flip();

        int result = 0;
        while (peerWriteBuffer.hasRemaining()) {
            result += channel.write(peerWriteBuffer);
        }
        clearBuffers();
        return result;
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            engine.closeOutbound();
            channel.close();
        }
    }

    private boolean isClosed() {
        return !channel.socket().isClosed() || isEngineClosed();
    }

    private boolean isEngineClosed() {
        return (engine.isOutboundDone() && engine.isInboundDone());
    }

    private void processDelegatedTask() throws IOException {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            task.run();
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        try {
            return channel.getLocalAddress();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }
}
