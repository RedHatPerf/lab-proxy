package org.horreum.perf.proxy.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.horreum.perf.proxy.proxy.ngrok.NgrokProxy;
import org.horreum.perf.proxy.proxy.smee.SmeeIoProxy;
import org.horreum.perf.proxy.services.MessageBus;

@Singleton
public class ProxyManager {
    private IProxy proxy;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MessageBus bus;

    @ConfigProperty(name = "proxy.service")
    String service;

    public void startup(@Observes StartupEvent startupEvent){

        switch (service){
            case "smee":
                proxy = new SmeeIoProxy();
                break;
            case "ngrok":
                proxy = new NgrokProxy();
                break;
            default:
                Log.warnf("Unknown proxy service: %s", service);
        }

        if ( proxy != null){
            proxy.start(objectMapper, bus);
        }

    }

    public void shutdown(@Observes ShutdownEvent shutdownEvent){
        if ( proxy != null){
            proxy.stop();
            proxy = null;
        }

    }

    public IProxy getProxy() {
        return proxy;
    }
}
