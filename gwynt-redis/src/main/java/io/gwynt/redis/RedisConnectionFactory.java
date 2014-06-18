package io.gwynt.redis;

public interface RedisConnectionFactory {

    RedisConnection getConnection();
}
