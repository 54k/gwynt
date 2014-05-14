package io.gwynt.core;

public interface ChannelCallback<T extends Channel> {

    void onComplete(T channel);

    void onError(T channel, Throwable e);
}
