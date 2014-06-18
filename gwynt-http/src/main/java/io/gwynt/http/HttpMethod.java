package io.gwynt.http;

import io.gwynt.core.util.AbstractConstant;
import io.gwynt.core.util.ConstantPool;

public class HttpMethod extends AbstractConstant<HttpMethod> {

    public static final HttpMethod GET = pool.valueOf("GET");
    public static final HttpMethod POST = pool.valueOf("POST");
    public static final HttpMethod PUT = pool.valueOf("PUT");
    public static final HttpMethod DELETE = pool.valueOf("DELETE");
    private static final ConstantPool<HttpMethod> pool = new ConstantPool<HttpMethod>() {
        @Override
        protected HttpMethod newConstant(int id, String name) {
            return new HttpMethod(id, name);
        }
    };

    private HttpMethod(int id, String name) {
        super(id, name);
    }
}
