package io.gwynt.core.pipeline;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.AbstractIoSession;
import io.gwynt.core.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPipeline implements Pipeline {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    private static final IoHandler HEAD_HANDLER = new HeadHandler();
    private static final IoHandler TAIL_HANDLER = new TailHandler();

    private final Object lock = new Object();
    private final DefaultIoHandlerContext head;
    private final DefaultIoHandlerContext tail;

    private final AbstractIoSession ioSession;

    public DefaultPipeline(AbstractIoSession ioSession) {
        this.ioSession = ioSession;
        head = new DefaultIoHandlerContext(ioSession, HEAD_HANDLER);
        tail = new DefaultIoHandlerContext(ioSession, TAIL_HANDLER);
        head.setNext(tail);
        head.setPrev(tail);
        tail.setPrev(head);
        tail.setNext(head);
    }

    @Override
    public void addHandler(IoHandler ioHandler) {
        DefaultIoHandlerContext handler = new DefaultIoHandlerContext(ioSession, ioHandler);
        synchronized (lock) {
            DefaultIoHandlerContext prev = tail.getPrev();
            prev.setNext(handler);
            handler.setNext(tail);
            handler.setPrev(prev);
            tail.setPrev(handler);
        }
    }

    public void fireOpen() {
        head.fireOpen();
    }

    public void fireMessageReceived(Object message) {
        head.fireMessageReceived(message);
    }

    public void fireClose() {
        head.fireClose();
    }

    public void fireExceptionCaught(Throwable e) {
        head.fireExceptionCaught(e);
    }

    @Override
    public void removeHandler(IoHandler ioHandler) {
        DefaultIoHandlerContext handlerNode = head.getNext();
        synchronized (lock) {
            do {
                Object h = handlerNode.getIoHandler();
                if (ioHandler == h) {
                    DefaultIoHandlerContext next = handlerNode.getNext();
                    DefaultIoHandlerContext prev = handlerNode.getPrev();
                    next.setPrev(prev);
                    prev.setNext(next);
                }
            } while ((handlerNode = handlerNode.getNext()) != null);
        }
    }

    private static class HeadHandler extends AbstractIoHandler<Object, byte[]> {

        @Override
        public void onMessageSent(IoHandlerContext context, byte[] message) {
            context.getIoSession().write(message);
        }

        @Override
        public void onClosing(IoHandlerContext context) {
            context.getIoSession().close();
        }
    }

    private static class TailHandler extends AbstractIoHandler {

        @Override
        public void onOpen(IoHandlerContext context) {
        }

        @Override
        public void onMessageReceived(IoHandlerContext context, Object message) {
        }

        @Override
        public void onMessageSent(IoHandlerContext context, Object message) {
        }

        @Override
        public void onClosing(IoHandlerContext context) {
        }

        @Override
        public void onClose(IoHandlerContext context) {
        }

        @Override
        public void onExceptionCaught(IoHandlerContext context, Throwable e) {
            logger.warn("Uncaught exception reached end of pipeline, check your pipeline configuration");
        }
    }
}
