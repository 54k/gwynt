package io.gwynt.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChannelOutboundBuffer {

    private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();

    public void addMessage(Object message, ChannelPromise channelPromise) {
        message = prepareMessage(message);
        Entry entry = newEntry();
        entry.message = message;
        entry.channelPromise = channelPromise;
        entries.add(entry);
    }

    protected Object prepareMessage(Object message) {
        return message;
    }

    protected Entry newEntry() {
        return new Entry();
    }

    public Object current() {
        Entry entry = entries.peek();
        if (entry != null) {
            return entry.message;
        }
        return null;
    }

    public void remove() {
        Entry entry = entries.poll();
        if (entry != null) {
            entry.channelPromise.complete();
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    protected static class Entry {
        private Object message;
        private ChannelPromise channelPromise;
    }
}