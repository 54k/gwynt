package io.gwynt.core.pipeline;

import io.gwynt.core.AbstractHandler;
import io.gwynt.core.Channel;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;
import io.gwynt.core.exception.PipelineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPipeline implements Pipeline, Iterable<DefaultHandlerContext> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    private static final Handler HEAD_HANDLER = new HeadHandler();
    private static final Handler TAIL_HANDLER = new TailHandler();

    private final Object lock = new Object();
    private final DefaultHandlerContext head;
    private final DefaultHandlerContext tail;

    private final Channel channel;
    private final Map<String, DefaultHandlerContext> name2context = new ConcurrentHashMap<>();

    public DefaultPipeline(Channel channel) {
        this.channel = channel;
        head = new DefaultHandlerContext(channel, HEAD_HANDLER);
        tail = new DefaultHandlerContext(channel, TAIL_HANDLER);
        head.setNext(tail);
        tail.setPrev(head);
    }

    private static String generateName(Handler handler) {
        return handler.getClass() + "@" + handler.hashCode();
    }

    private void checkMultiplicity(String name) {
        if (name2context.containsKey(name)) {
            throw new PipelineException("Duplicate name: " + name);
        }
    }

    public void fireRegistered() {
        head.fireRegistered();
    }

    public void fireUnregistered() {
        head.fireUnregistered();
    }

    public void fireOpen() {
        head.fireOpen();
    }

    public void fireMessageReceived(Object message) {
        head.fireMessageReceived(message);
    }

    public void fireRead(ChannelPromise channelPromise) {
        tail.read(channelPromise);
    }

    public void fireMessageSent(Object message, ChannelPromise channelPromise) {
        tail.write(message, channelPromise);
    }

    public void fireClosing(ChannelPromise channelPromise) {
        tail.close(channelPromise);
    }

    public void fireClose() {
        head.fireClose();
    }

    public void fireExceptionCaught(Throwable e) {
        head.fireExceptionCaught(e);
    }

    @Override
    public void addFirst(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addFirst(generateName(handler), handler);
    }

    @Override
    public void addFirst(String name, Handler handler) {
        if (name == null || handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext next = head.getNext();
            next.setPrev(context);
            context.setNext(next);
            context.setPrev(head);
            head.setNext(context);
            context.setName(name);
            context.removed = false;
            name2context.put(name, context);
            invokeHandlerAdded(context);
        }
    }

    @Override
    public void addLast(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addLast(generateName(handler), handler);
    }

    @Override
    public void addLast(String name, Handler handler) {
        if (name == null || handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext prev = tail.getPrev();
            prev.setNext(context);
            context.setNext(tail);
            context.setPrev(prev);
            tail.setPrev(context);
            context.setName(name);
            context.removed = false;
            name2context.put(name, context);
            invokeHandlerAdded(context);
        }
    }

    @Override
    public void addBefore(Handler handler, Handler before) {
        if (handler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultHandlerContext beforeContext = getContextOrDie(before);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(generateName(handler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(Handler handler, String beforeName) {
        if (handler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultHandlerContext beforeContext = getContextOrDie(beforeName);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(generateName(handler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(String name, Handler handler, Handler before) {
        if (name == null || handler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext beforeContext = getContextOrDie(before);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(String name, Handler handler, String beforeName) {
        if (name == null || handler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext beforeContext = getContextOrDie(beforeName);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    private void addBefore(DefaultHandlerContext context, DefaultHandlerContext before) {
        DefaultHandlerContext prev = before.getPrev();
        prev.setNext(context);
        context.setNext(before);
        context.setPrev(prev);
        before.setPrev(context);
        context.removed = false;
        name2context.put(context.name(), context);
        invokeHandlerAdded(context);
    }

    @Override
    public void addAfter(Handler handler, Handler after) {
        if (handler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultHandlerContext afterContext = getContextOrDie(after);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(generateName(handler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(Handler handler, String afterName) {
        if (handler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            DefaultHandlerContext afterContext = getContextOrDie(afterName);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(generateName(handler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(String name, Handler handler, Handler after) {
        if (name == null || handler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext afterContext = getContextOrDie(after);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(String name, Handler handler, String afterName) {
        if (name == null || handler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            DefaultHandlerContext afterContext = getContextOrDie(afterName);
            DefaultHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    private void addAfter(DefaultHandlerContext context, DefaultHandlerContext after) {
        DefaultHandlerContext next = after.getNext();
        next.setPrev(context);
        context.setNext(next);
        context.setPrev(after);
        after.setNext(context);
        context.removed = false;
        name2context.put(context.name(), context);
        invokeHandlerAdded(context);
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
    public void remove(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            remove(getContext(handler));
        }
    }

    private void remove(DefaultHandlerContext context) {
        if (context != null) {
            DefaultHandlerContext prev = context.getPrev();
            DefaultHandlerContext next = context.getNext();
            prev.setNext(context.getNext());
            next.setPrev(context.getPrev());
            context.removed = true;
            name2context.remove(context.name());
            invokeHandlerRemoved(context);
        }
    }

    @Override
    public void clear() {
        DefaultHandlerContext context = tail.getPrev();
        do {
            remove(context);
            context = context.getPrev();
        } while (context != null);
    }

    @Override
    public void copy(Pipeline pipeline) {
        clear();
        DefaultPipeline p = (DefaultPipeline) pipeline;
        for (DefaultHandlerContext ctx : p) {
            addFirst(ctx.name(), ctx.handler());
        }
    }

    private DefaultHandlerContext getContext(Handler handler) {
        DefaultHandlerContext contextByName;
        if ((contextByName = getContext(generateName(handler))) != null) {
            return contextByName;
        }
        for (DefaultHandlerContext context : this) {
            if (context.handler() == handler) {
                return context;
            }
        }
        return null;
    }

    private DefaultHandlerContext getContextOrDie(Handler handler) {
        DefaultHandlerContext contextByName;
        if ((contextByName = getContext(generateName(handler))) != null) {
            return contextByName;
        }
        for (DefaultHandlerContext context : this) {
            if (context.handler() == handler) {
                return context;
            }
        }
        throw new PipelineException("Context not found: " + handler);
    }

    private DefaultHandlerContext getContextOrDie(String name) {
        DefaultHandlerContext context = name2context.get(name);
        if (context == null) {
            throw new PipelineException("Context not found: " + name);
        }
        return context;
    }

    private DefaultHandlerContext getContext(String name) {
        return name2context.get(name);
    }

    @Override
    public Iterator<DefaultHandlerContext> iterator() {
        return new Iterator<DefaultHandlerContext>() {

            private DefaultHandlerContext context = head;

            @Override
            public boolean hasNext() {
                return context.getNext() != tail;
            }

            @Override
            public DefaultHandlerContext next() {
                DefaultHandlerContext next = context.getNext();
                return next != tail ? context = next : null;
            }

            @Override
            public void remove() {
                DefaultPipeline.this.remove(context);
            }
        };
    }

    private void invokeHandlerAdded(final DefaultHandlerContext context) {
        if (context.channel().isRegistered() && !context.channel().eventLoop().inExecutorThread()) {
            context.channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    context.handler().onHandlerAdded(context);
                }
            });
        }
        context.handler().onHandlerAdded(context);
    }

    private void invokeHandlerRemoved(final DefaultHandlerContext context) {
        if (context.channel().isRegistered() && !context.channel().eventLoop().inExecutorThread()) {
            context.channel().eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    context.handler().onHandlerRemoved(context);
                }
            });
        }
        context.handler().onHandlerRemoved(context);
    }

    private static class HeadHandler extends AbstractHandler {

        @Override
        public void onRead(HandlerContext context, ChannelPromise channelPromise) {
            context.channel().unsafe().read(channelPromise);
        }

        @Override
        public void onMessageSent(HandlerContext context, Object message, ChannelPromise channelPromise) {
            context.channel().unsafe().write(message, channelPromise);
        }

        @Override
        public void onClosing(HandlerContext context, ChannelPromise channelPromise) {
            context.channel().unsafe().close(channelPromise);
        }
    }

    private static class TailHandler extends AbstractHandler {

        @Override
        public void onRegistered(HandlerContext context) {
        }

        @Override
        public void onUnregistered(HandlerContext context) {
        }

        @Override
        public void onOpen(HandlerContext context) {
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
        }

        @Override
        public void onClose(HandlerContext context) {
        }

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            logger.warn("Uncaught exception reached end of pipeline, check your pipeline configuration");
            logger.warn("{}: {}", e.getClass().getName(), e.getMessage());
        }
    }
}
