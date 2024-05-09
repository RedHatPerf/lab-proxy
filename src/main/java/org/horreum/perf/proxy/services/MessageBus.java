package org.horreum.perf.proxy.services;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.horreum.perf.proxy.data.RequestPayload;

@ApplicationScoped
public class MessageBus {
    public static final String TOPIC = "ci-requests";

    @Inject
    EventBus bus;

    public void publish(@Nullable RequestPayload message) {
        bus.publish(TOPIC, message);
    }
}
