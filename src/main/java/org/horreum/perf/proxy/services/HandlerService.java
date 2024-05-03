package org.horreum.perf.proxy.services;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.horreum.perf.proxy.data.RequestPayload;

import java.io.IOException;

@ApplicationScoped
public class HandlerService {

    @Inject
    PayloadHandler handler;

    @ConsumeEvent("ci-requests")
    public void consume(RequestPayload event) {
        try {
            handler.handle(event);
        } catch (IOException e) {
            Log.error("Error handling event", e);
        }
    }
}
