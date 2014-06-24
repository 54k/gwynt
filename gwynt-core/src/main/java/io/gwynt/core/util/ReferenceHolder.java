package io.gwynt.core.util;

import java.util.concurrent.TimeUnit;

public abstract class ReferenceHolder<T> {

    private final Object lock = new Object();
    private T ref;
    private int refs;
    private final int maxRefs;
    private short waiters;

    protected ReferenceHolder() {
        this(Integer.MAX_VALUE);
    }

    protected ReferenceHolder(int maxRefs) {
        if (maxRefs <= 0) {
            throw new IllegalArgumentException("maxRefs > 0");
        }
        this.maxRefs = maxRefs;
    }

    public T acquire() {
        synchronized (lock) {
            while (refs == maxRefs) {
                incWaiters();
                try {
                    lock.wait();
                } catch (InterruptedException ignore) {
                } finally {
                    decWaiters();
                }
            }

            if (ref == null) {
                ref = newObject();
            }

            refs++;
        }

        return ref;
    }

    public T acquire(long timeout, TimeUnit timeUnit) {
        synchronized (lock) {
            while (refs == maxRefs) {
                incWaiters();
                try {
                    lock.wait(timeUnit.toMillis(timeout));
                } catch (InterruptedException ignore) {
                } finally {
                    decWaiters();
                }
            }

            if (ref == null) {
                ref = newObject();
            }

            refs++;
        }

        return ref;
    }

    public void release() {
        if (refs == 0) {
            throw new IllegalStateException("release() called before acquire()");
        }

        synchronized (lock) {
            refs--;
            if (hasWaiters()) {
                lock.notifyAll();
            }

            if (refs == 0) {
                try {
                    cleanupObject(ref);
                } finally {
                    if (releaseReference()) {
                        ref = null;
                    }
                }
            }
        }
    }

    private boolean hasWaiters() {
        return waiters > 0;
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("Too many waiters");
        }
        waiters++;
    }

    private void decWaiters() {
        waiters--;
    }

    public int refCount() {
        return refs;
    }

    protected boolean releaseReference() {
        return true;
    }

    protected abstract T newObject();

    protected abstract void cleanupObject(T reference);
}
