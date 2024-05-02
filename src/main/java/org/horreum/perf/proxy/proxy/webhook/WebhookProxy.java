package org.horreum.perf.proxy.proxy.webhook;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.horreum.perf.proxy.Handler;
import org.jboss.logging.Logger;

@Path("/")
public class WebhookProxy {

    private static final Logger LOG = Logger.getLogger(WebhookProxy.class);

    @Inject
    Handler handler;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public void webhook(String payload) {

        LOG.debugf("Received webhook call");

        handler.handle(payload);


    }
}
