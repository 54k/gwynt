package io.gwynt.core.pipeline;

import io.gwynt.core.Channel;
import io.gwynt.core.Channel.Unsafe;
import io.gwynt.core.ChannelPromise;
import io.gwynt.core.Handler;
import io.gwynt.core.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPipeline implements Pipeline, Iterable<AbstractHandlerContext> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPipeline.class);

    private final Object lock = new Object();
    private final AbstractHandlerContext head;
    private final AbstractHandlerContext tail;

    private final Channel channel;
    private final Map<String, AbstractHandlerContext> name2context = new ConcurrentHashMap<>();

    public DefaultPipeline(Channel channel) {
        this.channel = channel;
        head = new HeadHandler(channel);
        tail = new TailHandler(channel);
        head.setNext(tail);
        tail.setPrev(head);
    }

    private static String generateName(Handler handler) {
        return handler.getClass().getName() + "@" + handler.hashCode();
    }

    private void checkMultiplicity(String name) {
        if (name2context.containsKey(name)) {
            throw new PipelineException("Duplicate name: " + name);
        }
    }

    @Override
    public void fireRegistered() {
        head.fireRegistered();
    }

    @Override
    public void fireUnregistered() {
        head.fireUnregistered();
    }

    @Override
    public void fireOpen() {
        head.fireOpen();
    }

    @Override
    public void fireMessageReceived(Object message) {
        head.fireMessageReceived(message);
    }

    @Override
    public void fireRead(ChannelPromise channelPromise) {
        tail.read(channelPromise);
    }

    @Override
    public void fireMessageSent(Object message, ChannelPromise channelPromise) {
        tail.write(message, channelPromise);
    }

    @Override
    public void fireClosing(ChannelPromise channelPromise) {
        tail.close(channelPromise);
    }

    @Override
    public void fireClose() {
        head.fireClose();
    }

    @Override
    public void fireExceptionCaught(Throwable e) {
        head.fireExceptionCaught(e);
    }

    @Override
    public void addFirst(Handler handler) {
        addFirst((HandlerContextInvoker) null, handler);
    }

    @Override
    public void addFirst(String name, Handler handler) {
        addFirst((HandlerContextInvoker) null, name, handler);
    }

    @Override
    public void addFirst(EventExecutor executor, String name, Handler handler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addFirst(new DefaultHandlerContextInvoker(executor), name, handler);
    }

    @Override
    public void addFirst(HandlerContextInvoker invoker, String name, Handler handler) {
        if (name == null || handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext next = head.getNext();
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
    public void addFirst(EventExecutor executor, Handler handler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addFirst(new DefaultHandlerContextInvoker(executor), handler);
    }

    @Override
    public void addFirst(HandlerContextInvoker invoker, Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addFirst(invoker, generateName(handler), handler);
    }

    @Override
    public void addLast(Handler handler) {
        addLast((HandlerContextInvoker) null, handler);
    }

    @Override
    public void addLast(String name, Handler handler) {
        addLast((HandlerContextInvoker) null, name, handler);
    }

    @Override
    public void addLast(EventExecutor executor, String name, Handler handler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addLast(new DefaultHandlerContextInvoker(executor), name, handler);
    }

    @Override
    public void addLast(HandlerContextInvoker invoker, String name, Handler handler) {
        if (name == null || handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext prev = tail.getPrev();
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
    public void addLast(EventExecutor executor, Handler handler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addLast(new DefaultHandlerContextInvoker(executor), handler);
    }

    @Override
    public void addLast(HandlerContextInvoker invoker, Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        addLast(invoker, generateName(handler), handler);
    }

    @Override
    public void addBefore(Handler handler, Handler before) {
        addBefore((HandlerContextInvoker) null, handler, before);
    }

    @Override
    public void addBefore(Handler handler, String beforeName) {
        addBefore((HandlerContextInvoker) null, handler, beforeName);
    }

    @Override
    public void addBefore(String name, Handler handler, Handler before) {
        addBefore((HandlerContextInvoker) null, name, handler, before);
    }

    @Override
    public void addBefore(String name, Handler handler, String beforeName) {
        addBefore((HandlerContextInvoker) null, name, handler, beforeName);
    }

    @Override
    public void addBefore(EventExecutor executor, String name, Handler handler, String beforeName) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addBefore(new DefaultHandlerContextInvoker(executor), name, handler, beforeName);
    }

    @Override
    public void addBefore(HandlerContextInvoker invoker, String name, Handler handler, String beforeName) {
        if (name == null || handler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext beforeContext = getContextOrDie(beforeName);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(EventExecutor executor, String name, Handler handler, Handler before) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addBefore(new DefaultHandlerContextInvoker(executor), name, handler, before);
    }

    @Override
    public void addBefore(HandlerContextInvoker invoker, String name, Handler handler, Handler before) {
        if (name == null || handler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext beforeContext = getContextOrDie(before);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(name);
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(EventExecutor executor, Handler handler, String beforeName) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addBefore(new DefaultHandlerContextInvoker(executor), handler, beforeName);
    }

    @Override
    public void addBefore(HandlerContextInvoker invoker, Handler handler, String beforeName) {
        if (handler == null || beforeName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            AbstractHandlerContext beforeContext = getContextOrDie(beforeName);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(generateName(handler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    @Override
    public void addBefore(EventExecutor executor, Handler handler, Handler before) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addBefore(new DefaultHandlerContextInvoker(executor), handler, before);
    }

    @Override
    public void addBefore(HandlerContextInvoker invoker, Handler handler, Handler before) {
        if (handler == null || before == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            AbstractHandlerContext beforeContext = getContextOrDie(before);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(generateName(handler));
            if (beforeContext != null) {
                addBefore(context, beforeContext);
            }
        }
    }

    private void addBefore(AbstractHandlerContext context, AbstractHandlerContext before) {
        AbstractHandlerContext prev = before.getPrev();
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
        addAfter((HandlerContextInvoker) null, handler, after);
    }

    @Override
    public void addAfter(Handler handler, String afterName) {
        addAfter((HandlerContextInvoker) null, handler, afterName);
    }

    @Override
    public void addAfter(String name, Handler handler, Handler after) {
        addAfter((HandlerContextInvoker) null, name, handler, after);
    }

    @Override
    public void addAfter(String name, Handler handler, String afterName) {
        addAfter((HandlerContextInvoker) null, name, handler, afterName);
    }

    @Override
    public void addAfter(EventExecutor executor, String name, Handler handler, String afterName) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addAfter(new DefaultHandlerContextInvoker(executor), name, handler, afterName);
    }

    @Override
    public void addAfter(HandlerContextInvoker invoker, String name, Handler handler, String afterName) {
        if (name == null || handler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext afterContext = getContextOrDie(afterName);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(EventExecutor executor, String name, Handler handler, Handler after) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addAfter(new DefaultHandlerContextInvoker(executor), name, handler, after);
    }

    @Override
    public void addAfter(HandlerContextInvoker invoker, String name, Handler handler, Handler after) {
        if (name == null || handler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            checkMultiplicity(name);
            AbstractHandlerContext afterContext = getContextOrDie(after);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(name);
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(EventExecutor executor, Handler handler, String afterName) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addAfter(new DefaultHandlerContextInvoker(executor), handler, afterName);
    }

    @Override
    public void addAfter(HandlerContextInvoker invoker, Handler handler, String afterName) {
        if (handler == null || afterName == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            AbstractHandlerContext afterContext = getContextOrDie(afterName);
            AbstractHandlerContext context = new DefaultHandlerContext(invoker, channel, handler);
            context.setName(generateName(handler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    @Override
    public void addAfter(EventExecutor executor, Handler handler, Handler after) {
        if (executor == null) {
            throw new IllegalArgumentException("executor");
        }
        addAfter(new DefaultHandlerContextInvoker(executor), handler, after);
    }

    @Override
    public void addAfter(HandlerContextInvoker invoker, Handler handler, Handler after) {
        if (handler == null || after == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        synchronized (lock) {
            AbstractHandlerContext afterContext = getContextOrDie(after);
            AbstractHandlerContext context = new DefaultHandlerContext(channel, handler);
            context.setName(generateName(handler));
            if (afterContext != null) {
                addAfter(context, afterContext);
            }
        }
    }

    private void addAfter(AbstractHandlerContext context, AbstractHandlerContext after) {
        AbstractHandlerContext next = after.getNext();
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

    private void remove(AbstractHandlerContext context) {
        if (context != null) {
            AbstractHandlerContext prev = context.getPrev();
            AbstractHandlerContext next = context.getNext();
            prev.setNext(context.getNext());
            next.setPrev(context.getPrev());
            context.removed = true;
            name2context.remove(context.name());
            invokeHandlerRemoved(context);
        }
    }

    @Override
    public void clear() {
        AbstractHandlerContext context = tail.getPrev();
        do {
            remove(context);
            context = context.getPrev();
        } while (context != null);
    }

    @Override
    public void copy(Pipeline pipeline) {
        clear();
        DefaultPipeline p = (DefaultPipeline) pipeline;
        for (AbstractHandlerContext ctx : p) {
            addFirst(ctx.name(), ctx.handler());
        }
    }

    private AbstractHandlerContext getContext(Handler handler) {
        AbstractHandlerContext contextByName;
        if ((contextByName = getContext(generateName(handler))) != null) {
            return contextByName;
        }
        for (AbstractHandlerContext context : this) {
            if (context.handler() == handler) {
                return context;
            }
        }
        return null;
    }

    private AbstractHandlerContext getContextOrDie(Handler handler) {
        AbstractHandlerContext contextByName;
        if ((contextByName = getContext(generateName(handler))) != null) {
            return contextByName;
        }
        for (AbstractHandlerContext context : this) {
            if (context.handler() == handler) {
                return context;
            }
        }
        throw new PipelineException("Context not found: " + handler);
    }

    private AbstractHandlerContext getContextOrDie(String name) {
        AbstractHandlerContext context = name2context.get(name);
        if (context == null) {
            throw new PipelineException("Context not found: " + name);
        }
        return context;
    }

    private AbstractHandlerContext getContext(String name) {
        return name2context.get(name);
    }

    @Override
    public Iterator<AbstractHandlerContext> iterator() {
        return new Iterator<AbstractHandlerContext>() {

            private AbstractHandlerContext context = head;

            @Override
            public boolean hasNext() {
                return context.getNext() != tail;
            }

            @Override
            public AbstractHandlerContext next() {
                AbstractHandlerContext next = context.getNext();
                return next != tail ? context = next : null;
            }

            @Override
            public void remove() {
                DefaultPipeline.this.remove(context);
            }
        };
    }

    private void invokeHandlerAdded(final AbstractHandlerContext context) {
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

    private void invokeHandlerRemoved(final AbstractHandlerContext context) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("(contexts: ");
        for (Iterator<Entry<String, AbstractHandlerContext>> iterator = name2context.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<String, AbstractHandlerContext> entry = iterator.next();
            sb.append(entry.getValue());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }

    private static class HeadHandler extends AbstractHandlerContext implements Handler {

        private Unsafe unsafe;

        private HeadHandler(Channel channel) {
            super(channel);
            unsafe = channel.unsafe();
        }

        @Override
        public void onHandlerAdded(HandlerContext context) {
        }

        @Override
        public void onHandlerRemoved(HandlerContext context) {
        }

        @Override
        public void onRegistered(HandlerContext context) {
            context.fireRegistered();
        }

        @Override
        public void onUnregistered(HandlerContext context) {
            context.fireUnregistered();
        }

        @Override
        public void onOpen(HandlerContext context) {
            context.fireOpen();
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
            context.fireMessageReceived(message);
        }

        @Override
        public void onClose(HandlerContext context) {
            context.fireClose();
        }

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            context.fireExceptionCaught(e);
        }

        @Override
        public Handler handler() {
            return this;
        }

        @Override
        public void onRead(HandlerContext context, ChannelPromise channelPromise) {
            unsafe.read(channelPromise);
        }

        @Override
        public void onMessageSent(HandlerContext context, Object message, ChannelPromise channelPromise) {
            unsafe.write(message, channelPromise);
        }

        @Override
        public void onClosing(HandlerContext context, ChannelPromise channelPromise) {
            unsafe.close(channelPromise);
        }

        @Override
        public void onDisconnect(HandlerContext context, ChannelPromise channelPromise) {
            unsafe.disconnect(channelPromise);
        }
    }

    private static class TailHandler extends AbstractHandlerContext implements Handler {

        private TailHandler(Channel channel) {
            super(channel);
        }

        @Override
        public void onHandlerAdded(HandlerContext context) {
        }

        @Override
        public void onHandlerRemoved(HandlerContext context) {
        }

        @Override
        public Handler handler() {
            return this;
        }

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
        public void onRead(HandlerContext context, ChannelPromise channelPromise) {
        }

        @Override
        public void onMessageSent(HandlerContext context, Object message, ChannelPromise channelPromise) {
        }

        @Override
        public void onMessageReceived(HandlerContext context, Object message) {
        }

        @Override
        public void onClosing(HandlerContext context, ChannelPromise channelPromise) {
        }

        @Override
        public void onDisconnect(HandlerContext context, ChannelPromise channelPromise) {
        }

        @Override
        public void onClose(HandlerContext context) {
        }

        @Override
        public void onExceptionCaught(HandlerContext context, Throwable e) {
            logger.warn("Uncaught exception reached end of pipeline, check your pipeline configuration");
            logger.warn(e.getClass().getName(), e);
        }
    }
}
