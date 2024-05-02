package org.horreum.perf.proxy.proxy.smee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.horreum.perf.proxy.Handler;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.proxy.smee.sse.EventStreamListener;
import org.horreum.perf.proxy.proxy.smee.sse.HttpEventStreamClient;
import org.jboss.logging.Logger;

import java.net.http.HttpResponse;


@ApplicationScoped
@Startup
public class SmeeIoProxy {



    private static final Logger LOG = Logger.getLogger(SmeeIoProxy.class);

    private static final String EMPTY_MESSAGE = "{}";

    private final HttpEventStreamClient eventStreamClient;
    private final ReplayEventStreamAdapter replayEventStreamAdapter;

    private static final String PROXY_URL = "https://smee.io/YSjZRcvD6VtIqYS";

    @Inject
    Handler handler;

    @Inject
    SmeeIoProxy(ObjectMapper objectMapper) {

        LOG.info("Listening to events coming from " + PROXY_URL);

//        URI localUrl = URI.create("http://" + httpConfiguration.host + ":" + httpConfiguration.port + "/");

        this.replayEventStreamAdapter = new ReplayEventStreamAdapter(PROXY_URL,
                objectMapper);
        this.eventStreamClient = new HttpEventStreamClient(PROXY_URL,
                this.replayEventStreamAdapter);
        this.eventStreamClient.setRetryCooldown(3000);
        this.eventStreamClient.start();
    }

    void stopEventSource(@Observes ShutdownEvent shutdownEvent) {
        if (this.replayEventStreamAdapter != null) {
            this.replayEventStreamAdapter.stop();
        }
        if (this.eventStreamClient != null) {
            this.eventStreamClient.stop();
        }
    }

    private class ReplayEventStreamAdapter implements EventStreamListener {

        private final String proxyUrl;
//        private final URI localUrl;
        private final ObjectMapper objectMapper;
//        private final HttpClient forwardingHttpClient;

        private volatile boolean stopped = false;

        private ReplayEventStreamAdapter(String proxyUrl, ObjectMapper objectMapper) {
            this.proxyUrl = proxyUrl;
//            this.localUrl = localUrl;
            this.objectMapper = objectMapper;
//            this.forwardingHttpClient = HttpClient.newBuilder()
//                    .connectTimeout(Duration.ofSeconds(2))
//                    .build();
        }

        @Override
        public void onEvent(HttpEventStreamClient client, HttpEventStreamClient.Event event) {
            if (stopped) {
                return;
            }

            if (EMPTY_MESSAGE.equals(event.getData())) {
                return;
            }

            int startOfJsonObject = event.getData().indexOf('{');
            if (startOfJsonObject == -1) {
                return;
            }

            // for some reason, the message coming from smee.io sometimes includes a 'id: 123' at the beginning of the message
            // let's be safe and drop anything before the start of the JSON object.
            String data = event.getData().substring(startOfJsonObject);

            try {
                LOG.debugf("Received event: %s", event.toString());
                JsonNode rootNode = objectMapper.readTree(data);
                RequestPayload payload = objectMapper.convertValue(rootNode.get("body"), RequestPayload.class) ;


                if (payload != null) {
                    LOG.debugf("Received body: %s", payload.toString());

                    handler.handle(payload);
/*
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(localUrl)
                            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(rootNode.get("body"))));

                    for (String forwardedHeader : FORWARDED_HEADERS) {
                        JsonNode headerValue = rootNode.get(forwardedHeader.toLowerCase(Locale.ROOT));
                        if (headerValue != null && headerValue.isTextual()) {
                            requestBuilder.header(forwardedHeader, headerValue.textValue());
                        }
                    }

                    forwardingHttpClient.send(requestBuilder.build(), BodyHandlers.discarding());
*/
                }
            } catch (Exception e) {
                LOG.error("An error occurred while forwarding a payload to the local application running in dev mode", e);
            }
        }

        @Override
        public void onReconnect(HttpEventStreamClient client, HttpResponse<Void> response, boolean hasReceivedEvents,
                long lastEventID) {
            if (stopped) {
                return;
            }

            LOG.info("Reconnected to " + proxyUrl);
        }

        @Override
        public void onError(HttpEventStreamClient client, Throwable throwable) {
            if (stopped) {
                return;
            }

            LOG.error("An error occurred with Smee.io proxying", throwable);
        }

        @Override
        public void onClose(HttpEventStreamClient client, HttpResponse<Void> response) {
        }

        public void stop() {
            this.stopped = true;
        }
    }
}
