package org.horreum.perf.proxy.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.horreum.perf.proxy.services.MessageBus;

public interface IProxy {

    void start(ObjectMapper objectMapper, MessageBus messageBus);

    void stop();

    boolean isRunning();

}
