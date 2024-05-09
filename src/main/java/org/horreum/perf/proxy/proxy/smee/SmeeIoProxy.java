package org.horreum.perf.proxy.proxy.smee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.proxy.IProxy;
import org.horreum.perf.proxy.proxy.smee.sse.EventStreamListener;
import org.horreum.perf.proxy.proxy.smee.sse.HttpEventStreamClient;
import org.horreum.perf.proxy.services.MessageBus;
import org.horreum.perf.proxy.services.PayloadHandler;
import org.jboss.logging.Logger;

import java.net.http.HttpResponse;
import java.util.Optional;


public class SmeeIoProxy implements IProxy {

    @ConfigProperty(name = "proxy.smee.uid")
    Optional<String> uid;

    private static final Logger LOG = Logger.getLogger(SmeeIoProxy.class);

    private static final String EMPTY_MESSAGE = "{}";

    private HttpEventStreamClient eventStreamClient;
    private ReplayEventStreamAdapter replayEventStreamAdapter;

    private static final String PROXY_URL = "https://smee.io/";

    private boolean running = false;

    MessageBus messageBus;

    ObjectMapper objectMapper;


    @Override
    public void start(ObjectMapper objectMapper, MessageBus messageBus) {

        if ( uid == null || uid.isEmpty() ) {
            LOG.infof("smee.io proxy not configured, skipping");
            return;
        }

        this.messageBus = messageBus;
        this.objectMapper = objectMapper;

        String smeeUrl = PROXY_URL.concat(uid.get());
        LOG.info("Listening to events coming from " + smeeUrl);

//        URI localUrl = URI.create("http://" + httpConfiguration.host + ":" + httpConfiguration.port + "/");

        this.replayEventStreamAdapter = new ReplayEventStreamAdapter(smeeUrl,
                objectMapper);
        this.eventStreamClient = new HttpEventStreamClient(smeeUrl,
                this.replayEventStreamAdapter);
        this.eventStreamClient.setRetryCooldown(3000);
        this.eventStreamClient.start();
        running = true;
    }

    @Override
    public void stop() {
        if (this.replayEventStreamAdapter != null) {
            this.replayEventStreamAdapter.stop();
        }
        if (this.eventStreamClient != null) {
            this.eventStreamClient.stop();
        }

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
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

                    messageBus.publish(payload);
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
