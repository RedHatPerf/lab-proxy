package org.horreum.perf.proxy.proxy.ngrok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngrok.Http;
import com.ngrok.Listener;
import com.ngrok.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.ConfigProvider;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.proxy.IProxy;
import org.horreum.perf.proxy.services.MessageBus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class NgrokProxy implements IProxy {

    //    @ConfigProperty(name = "proxy.ngrok.authToken")
    String authToken;

    //    @ConfigProperty(name = "proxy.ngrok.user")
    String user;

    //    @ConfigProperty(name = "proxy.ngrok.userToken")
    String userToken;

    String domain;

    private static final byte[] RESPONSE_OK = "HTTP/1.0 200 OK\n\nOK".getBytes(Charset.forName("UTF-8"));
    private static final byte[] RESPONSE_METHOD_NOT_ALLOWED = "HTTP/1.1 405 Method Not Allowed\nContent-Type: text/html\nAllow: POST\n\n<h1>405 Try another method!</h1>\n".getBytes(Charset.forName("UTF-8"));

    private static final byte[] RESPONSE_INTERNAL_SERVER_ERROR = "HTTP/1.1 500 Internal Server Error\n\nContent-Type: text/html\nAllow: POST\n\n<h1>500 Internal Server Error</h1>\n".getBytes(Charset.forName("UTF-8"));

    private Thread NgrokListenerThread;
    private AtomicReference<MessageBus> messageBus = new AtomicReference<>();
    private AtomicReference<Session> ngrokSession = new AtomicReference<>();
    private AtomicReference<Listener.Endpoint> ngrokListener = new AtomicReference<>();

    @Override
    public void start(ObjectMapper objectMapper, MessageBus messageBus) {

        Log.info("Starting Ngrok Proxy");

        //retrive config - we can not use injection as this is not a managed bean
        //TODO:: create profiles for managed beans
        authToken = ConfigProvider.getConfig().getValue("proxy.ngrok.authToken", String.class);
        user = ConfigProvider.getConfig().getValue("proxy.ngrok.user", String.class);
        userToken = ConfigProvider.getConfig().getValue("proxy.ngrok.userToken", String.class);
        domain = ConfigProvider.getConfig().getValue("proxy.ngrok.domain", String.class);

        HttpServerRequestDecoder decoder = new HttpServerRequestDecoder();

        this.messageBus.set(messageBus);

        NgrokListenerThread = new Thread(() -> {
            final var sessionBuilder = Session.withAuthtoken(authToken);

            try (final var session = sessionBuilder.connect()) {
                this.ngrokSession.set(session); //reference to session for cleanup

                final var listenerBuilder = session
                        .httpEndpoint()
                        .domain(domain)
                        .basicAuthOptions(new Http.BasicAuth(user, userToken));

                try (final var listener = listenerBuilder.listen()) {
                    ngrokListener.set(listener);
                    Log.infof("ngrok url: %s", listener.getUrl());
                    final var buf = ByteBuffer.allocateDirect(1024);

                    while (true) {
                        // Accept a new connection
                        final var conn = listener.accept();
                        byte[] response;

                        // Read from the connection
                        buf.clear();
                        conn.read(buf);
                        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf.duplicate());
                        try {
                            List<Object> out = new ArrayList<>();
                            //decode the HTTP request Headers
                            decoder.decode(null, byteBuf, out);

                            if (out.size() == 0 || !(out.get(0) instanceof HttpRequest)) {
                                Log.error("Could not decode the request");
                                Log.debug(Charset.forName("UTF-8").decode(buf));
                                response = RESPONSE_INTERNAL_SERVER_ERROR;
                            } else {

                                HttpRequest httpRequest = (HttpRequest) out.get(0);

                                if (!httpRequest.method().name().equals("POST")) {
                                    response = RESPONSE_METHOD_NOT_ALLOWED;
                                } else {
                                    //parse message
                                    StringBuilder payload = new StringBuilder();
                                    buf.rewind();
                                    boolean foundNewLine = false;
                                    boolean foundBody = false;
                                    while (buf.hasRemaining()) {
                                        //todo without the char cast, the byte is treated as a signed byte
                                        char c = (char) buf.get();
                                        if (foundBody) {
                                            payload.append(c);
                                        } else if (c == '\n') {
                                            if (foundNewLine) {
                                                foundBody = true;
                                            } else {
                                                foundNewLine = true;
                                            }
                                        } else if (c == '\r') {
                                            //ignore
                                        } else {
                                            foundNewLine = false;
                                        }
                                    }

                                    String msg = payload.toString().trim();
                                    Log.infof("Received message: %s", msg);
                                    RequestPayload requestPayload = objectMapper.readValue(msg, RequestPayload.class);
                                    requestPayload.timestamp = Instant.now();
                                    this.messageBus.get().publish(requestPayload);

                                    response = RESPONSE_OK;
                                }
                            }

                            // Send Response
                            buf.clear();
                            buf.put(response);
                            buf.flip();
                            conn.write(buf);
                            conn.close();

                        } catch (Exception e) {
                            Log.error("Could not close parse http request", e);
                        }

                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        NgrokListenerThread.start();

    }

    @Override
    public void stop() {
        try {
            Listener.Endpoint listener = this.ngrokListener.get();
            if (listener != null) {
                listener.close();
            }

            Session session = this.ngrokSession.get();
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            Log.error("Could not close ngrok session", e);
        }

    }

    @Override
    public boolean isRunning() {
        return ngrokListener.get() != null;
    }

    private String parseMsg(ByteBuffer buf) {
        return Charset.forName("UTF-8").decode(buf).toString();
    }

    static final class HttpServerRequestDecoder extends HttpRequestDecoder {
        protected HttpServerRequestDecoder() {
            super();
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
            super.decode(ctx, buffer, out);
        }
    }
}
