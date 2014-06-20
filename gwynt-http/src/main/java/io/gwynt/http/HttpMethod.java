package io.gwynt.http;

import io.gwynt.core.util.AbstractConstant;
import io.gwynt.core.util.ConstantPool;

final class HttpMethod extends AbstractConstant<HttpMethod> {

    private static final ConstantPool<HttpMethod> pool;

    public static final HttpMethod GET;
    public static final HttpMethod POST;
    public static final HttpMethod PUT;
    public static final HttpMethod DELETE;

    static {
        pool = new ConstantPool<HttpMethod>() {
            @Override
            protected HttpMethod newConstant(int id, String name) {
                return new HttpMethod(id, name);
            }
        };

        GET = pool.valueOf("GET");
        POST = pool.valueOf("POST");
        PUT = pool.valueOf("PUT");
        DELETE = pool.valueOf("DELETE");
    }

    private HttpMethod(int id, String name) {
        super(id, name);
    }

    static HttpMethod valueOf(String name) {
        return pool.valueOf(name);
    }
}
