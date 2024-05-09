package org.horreum.perf.proxy.proxy.ngrok;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestHttpHandler {

    private static final byte[] sampleRequest = """
            POST / HTTP/1.1
            Host: localhost:8080
            User-Agent: curl/7.68.0
            Accept: */*
            Content-Length: 90
            Content-Type: application/json
            X-Forwarded-For: 2a0d:3344:185:a810::f02
            X-Forwarded-Host: b8f1-2a0d-3344-185-a810-00-f02.ngrok-free.app
            X-Forwarded-Proto: https
            Accept-Encoding: gzip

            {"jobName":"horreum-healthcheck","parameters":{ "HOST": "localhost", "USER": "unknown-user" }}
            """.getBytes(Charset.forName("UTF-8"));
    @Test
    public void testHttpHandler(){

        ByteBuf byteBuf =  Unpooled.wrappedBuffer(sampleRequest);

        NgrokProxy.HttpServerRequestDecoder decoder = new NgrokProxy.HttpServerRequestDecoder();
        List<Object> out = new ArrayList<>();

        try {
            decoder.decode(null, byteBuf, out);

            assertEquals(1, out.size());

            HttpRequest httpRequest = (HttpRequest) out.get(0);

            assertEquals("POST", httpRequest.method().name());

        } catch (Exception e) {
            fail(e);
        }

    }
}
