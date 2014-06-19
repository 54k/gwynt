package io.gwynt.http;

import io.gwynt.core.util.AbstractConstant;
import io.gwynt.core.util.ConstantPool;

final class HttpVersion extends AbstractConstant<HttpVersion> {

    public static final HttpVersion VERSION_10;
    public static final HttpVersion VERSION_11;
    private static final ConstantPool<HttpVersion> pool;

    static {
        pool = new ConstantPool<HttpVersion>() {
            @Override
            protected HttpVersion newConstant(int id, String name) {
                return new HttpVersion(id, name);
            }
        };

        VERSION_10 = pool.valueOf("HTTP/1.0");
        VERSION_11 = pool.valueOf("HTTP/1.1");
    }

    private HttpVersion(int id, String name) {
        super(id, name);
    }

    static HttpVersion valueOf(String name) {
        return pool.valueOf(name);
    }
}
