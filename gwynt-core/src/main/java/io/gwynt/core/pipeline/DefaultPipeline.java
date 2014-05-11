package io.gwynt.core.pipeline;

import io.gwynt.core.AbstractIoHandler;
import io.gwynt.core.AbstractIoSession;
import io.gwynt.core.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPipeline implements Pipeline, Iterable<DefaultIoHandlerContext> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    private static final IoHandler HEAD_HANDLER = new HeadHandler();
    private static final IoHandler TAIL_HANDLER = new TailHandler();

    private final Object lock = new Object();
    private final DefaultIoHandlerContext head;
    private final DefaultIoHandlerContext tail;

    private final AbstractIoSession ioSession;
    private final Map<String, DefaultIoHandlerContext> name2context = new ConcurrentHashMap<>();

    public DefaultPipeline(AbstractIoSession ioSession) {
        this.ioSession = ioSession;
        head = new DefaultIoHandlerContext(ioSession, HEAD_HANDLER);
        tail = new DefaultIoHandlerContext(ioSession, TAIL_HANDLER);
        head.setNext(tail);
        tail.setPrev(head);
    }

    private static String generateName(IoHandler ioHandler) {
        return ioHandler.getClass() + "@" + ioHandler.hashCode();
    }

    public void fireRegistered() {
        head.fireOnRegistered();
    }

    public void fireUnregistered() {
        head.fireOnUnregistered();
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
    public void addFirst(IoHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addFirst(generateName(ioHandler), ioHandler);
    }

    @Override
    public void addFirst(String name, IoHandler ioHandler) {
        if (name == null || ioHandler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
        synchronized (lock) {
            DefaultIoHandlerContext next = head.getNext();
            next.setPrev(context);
            context.setNext(next);
            context.setPrev(head);
            head.setNext(context);
            context.setName(name);
            context.removed = false;
            name2context.put(name, context);
            context.fireOnAdded();
        }
    }

    @Override
    public void addLast(IoHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addLast(generateName(ioHandler), ioHandler);
    }

    @Override
    public void addLast(String name, IoHandler ioHandler) {
        if (name == null || ioHandler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
        synchronized (lock) {
            DefaultIoHandlerContext prev = tail.getPrev();
            prev.setNext(context);
            context.setNext(tail);
            context.setPrev(prev);
            tail.setPrev(context);
            context.setName(name);
            context.removed = false;
            name2context.put(name, context);
            context.fireOnAdded();
        }
    }

    @Override
    public void addBefore(IoHandler ioHandler, IoHandler before) {
        if (ioHandler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext beforeContext = getContext(before);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(generateName(ioHandler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(IoHandler ioHandler, String beforeName) {
        if (ioHandler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext beforeContext = getContext(beforeName);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(generateName(ioHandler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(String name, IoHandler ioHandler, IoHandler before) {
        if (name == null || ioHandler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext beforeContext = getContext(before);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(String name, IoHandler ioHandler, String beforeName) {
        if (name == null || ioHandler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext beforeContext = getContext(beforeName);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    private void addBefore(DefaultIoHandlerContext context, DefaultIoHandlerContext before) {
        DefaultIoHandlerContext prev = before.getPrev();
        prev.setNext(context);
        context.setNext(before);
        context.setPrev(prev);
        before.setPrev(context);
        context.removed = false;
        name2context.put(context.getName(), context);
        context.fireOnAdded();
    }

    @Override
    public void addAfter(IoHandler ioHandler, IoHandler after) {
        if (ioHandler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext afterContext = getContext(after);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(generateName(ioHandler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(IoHandler ioHandler, String afterName) {
        if (ioHandler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext afterContext = getContext(afterName);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(generateName(ioHandler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(String name, IoHandler ioHandler, IoHandler after) {
        if (name == null || ioHandler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext afterContext = getContext(after);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(String name, IoHandler ioHandler, String afterName) {
        if (name == null || ioHandler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultIoHandlerContext afterContext = getContext(afterName);
            DefaultIoHandlerContext context = new DefaultIoHandlerContext(ioSession, ioHandler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    private void addAfter(DefaultIoHandlerContext context, DefaultIoHandlerContext after) {
        DefaultIoHandlerContext next = after.getNext();
        next.setPrev(context);
        context.setNext(next);
        context.setPrev(after);
        after.setNext(context);
        context.removed = false;
        name2context.put(context.getName(), context);
        context.fireOnAdded();
    }

    @Override
    public void remove(String name) {
        if (name == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            remove(getContext(name));
        }
    }

    @Override
    public void remove(IoHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            remove(getContext(ioHandler));
        }
    }

    private void remove(DefaultIoHandlerContext context) {
        if (context != null) {
            DefaultIoHandlerContext prev = context.getPrev();
            DefaultIoHandlerContext next = context.getNext();
            prev.setNext(context.getNext());
            next.setPrev(context.getPrev());
            context.removed = true;
            name2context.remove(context.getName());
            context.fireOnRemoved();
        }
    }

    private DefaultIoHandlerContext getContext(IoHandler ioHandler) {
        DefaultIoHandlerContext contextByName;
        if ((contextByName = getContext(generateName(ioHandler))) != null) {
            return contextByName;
        }
        for (DefaultIoHandlerContext context : this) {
            if (context.getIoHandler() == ioHandler) {
                return context;
            }
        }
        return null;
    }

    private DefaultIoHandlerContext getContext(String name) {
        return name2context.get(name);
    }

    @Override
    public Iterator<DefaultIoHandlerContext> iterator() {
        return new Iterator<DefaultIoHandlerContext>() {

            private DefaultIoHandlerContext context = head;

            @Override
            public boolean hasNext() {
                return context.getNext() != tail;
            }

            @Override
            public DefaultIoHandlerContext next() {
                DefaultIoHandlerContext next = context.getNext();
                return next != tail ? context = next : null;
            }
        };
    }

    private static class HeadHandler extends AbstractIoHandler {

        @Override
        public void onMessageSent(IoHandlerContext context, Object message) {
            context.getIoSession().write(message);
        }

        @Override
        public void onClosing(IoHandlerContext context) {
            context.getIoSession().close();
        }
    }

    private static class TailHandler extends AbstractIoHandler {

        @Override
        public void onRegistered(IoHandlerContext context) {
        }

        @Override
        public void onUnregistered(IoHandlerContext context) {
        }

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
