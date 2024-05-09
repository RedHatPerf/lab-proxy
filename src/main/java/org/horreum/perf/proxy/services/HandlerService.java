package org.horreum.perf.proxy.services;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.horreum.perf.proxy.data.RequestPayload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class HandlerService {

    private Queue<RequestPayload> requestPayloadList = new ConcurrentLinkedQueue<>();

    @Inject
    PayloadHandler handler;

    @ConsumeEvent("ci-requests")
    public void consume(RequestPayload event) {
        try {
            handler.handle(event);
            addRequest(event);
        } catch (IOException e) {
            Log.error("Error handling event", e);
        }
    }

    private void addRequest(RequestPayload requestPayload) {
        requestPayloadList.add(requestPayload);
    }

    public List<RequestPayload> getRequests() {
        return new ArrayList<>(requestPayloadList);
    }
}
