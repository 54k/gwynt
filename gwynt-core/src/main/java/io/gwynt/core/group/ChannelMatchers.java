package io.gwynt.core.group;

import io.gwynt.core.Channel;
import io.gwynt.core.ServerChannel;

public final class ChannelMatchers {

    private static ChannelMatcher ALL_MATCHER = new ChannelMatcher() {
        @Override
        public boolean match(Channel channel) {
            return true;
        }
    };
    private static ChannelMatcher SERVER_CHANNELS = isInstanceOf(ServerChannel.class);
    private static ChannelMatcher NON_SERVER_CHANNELS = isNotInstanceOf(ServerChannel.class);

    private ChannelMatchers() {
    }

    public static ChannelMatcher all() {
        return ALL_MATCHER;
    }

    public static ChannelMatcher isInstanceOf(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new IllegalArgumentException("channelClass");
        }
        return new ClassMatcher(channelClass);
    }

    public static ChannelMatcher isNotInstanceOf(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new IllegalArgumentException("channelClass");
        }
        return invert(new ClassMatcher(channelClass));
    }

    public static ChannelMatcher is(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        return new InstanceMatcher(channel);
    }

    public static ChannelMatcher isNot(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel");
        }
        return invert(new InstanceMatcher(channel));
    }

    public static ChannelMatcher isServerChannel() {
        return SERVER_CHANNELS;
    }

    public static ChannelMatcher isNotServerChannel() {
        return NON_SERVER_CHANNELS;
    }

    public static ChannelMatcher invert(ChannelMatcher matcher) {
        if (matcher == null) {
            throw new IllegalArgumentException("matcher");
        }
        return new InvertMatcher(matcher);
    }

    public static ChannelMatcher compose(ChannelMatcher... matchers) {
        if (matchers.length < 1) {
            throw new IllegalArgumentException("matchers must at least contain one element");
        }
        if (matchers.length == 1) {
            return matchers[0];
        }
        return new CompositeMatcher(matchers);
    }

    private static final class CompositeMatcher implements ChannelMatcher {
        private final ChannelMatcher[] matchers;

        CompositeMatcher(ChannelMatcher... matchers) {
            this.matchers = matchers;
        }

        @Override
        public boolean match(Channel channel) {
            for (ChannelMatcher matcher : matchers) {
                if (!matcher.match(channel)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class InvertMatcher implements ChannelMatcher {

        private ChannelMatcher matcher;

        private InvertMatcher(ChannelMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean match(Channel channel) {
            return !matcher.match(channel);
        }
    }

    private static class InstanceMatcher implements ChannelMatcher {

        private Channel channel;

        private InstanceMatcher(Channel channel) {
            this.channel = channel;
        }

        @Override
        public boolean match(Channel channel) {
            return this.channel == channel;
        }
    }

    private static class ClassMatcher implements ChannelMatcher {

        private Class<? extends Channel> channelClass;

        private ClassMatcher(Class<? extends Channel> channelClass) {
            this.channelClass = channelClass;
        }

        @Override
        public boolean match(Channel channel) {
            return channelClass.isInstance(channel);
        }
    }
}
