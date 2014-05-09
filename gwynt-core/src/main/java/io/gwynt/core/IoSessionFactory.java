package io.gwynt.core;

public interface IoSessionFactory<I, O extends IoSession> {

    O createConnection(I channel);
}
