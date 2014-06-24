package io.gwynt.core;

import io.gwynt.core.concurrent.AbstractFuture;
import io.gwynt.core.concurrent.Future;
import io.gwynt.core.concurrent.FutureListener;
import io.gwynt.core.concurrent.Promise;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class VoidChannelPromise extends AbstractFuture<Void> implements ChannelPromise {

    private Channel channel;

    VoidChannelPromise(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean trySuccess() {
        return true;
    }

    @Override
    public ChannelPromise setSuccess() {
        return this;
    }

    @Override
    public ChannelPromise setSuccess(Void result) {
        return this;
    }

    @Override
    public ChannelPromise setFailure(Throwable error) {
        fireExceptionCaught(error);
        return this;
    }

    @Override
    public ChannelPromise chainPromises(Promise<Void>... promises) {
        fail();
        return this;
    }

    @Override
    public ChannelPromise chainPromise(Promise<Void> promise) {
        fail();
        return this;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public ChannelFuture addListener(FutureListener<? extends Future<? super Void>> futureListener) {
        fail();
        return this;
    }

    @Override
    public ChannelFuture addListeners(FutureListener<? extends Future<? super Void>>... futureListeners) {
        fail();
        return this;
    }

    @Override
    public ChannelFuture removeListener(FutureListener<? extends Future<? super Void>> futureListener) {
        return this;
    }

    @Override
    public ChannelFuture removeListeners(FutureListener<? extends Future<? super Void>>... futureListeners) {
        return this;
    }

    @Override
    public ChannelFuture await() throws InterruptedException {
        return this;
    }

    @Override
    public boolean setUncancellable() {
        return false;
    }

    @Override
    public boolean trySuccess(Void result) {
        return true;
    }

    @Override
    public boolean tryFailure(Throwable error) {
        return true;
    }

    @Override
    public boolean isUncancellable() {
        return true;
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public Void getNow() {
        return null;
    }

    @Override
    public Throwable getCause() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public Void get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public ChannelPromise sync() throws InterruptedException {
        return this;
    }

    @Override
    public ChannelPromise sync(long timeout, TimeUnit unit) throws InterruptedException {
        return this;
    }

    @Override
    public ChannelPromise sync(long timeoutMillis) throws InterruptedException {
        return this;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    private static void fail() {
        throw new IllegalArgumentException("void future");
    }

    private void fireExceptionCaught(Throwable t) {
        if (channel.isRegistered()) {
            channel.pipeline().fireExceptionCaught(t);
        }
    }
}
