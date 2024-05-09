package org.horreum.perf.proxy.proxy.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.services.MessageBus;
import org.jboss.logging.Logger;

import java.io.IOException;

@Path("/")
public class WebhookProxy {

    private static final Logger LOG = Logger.getLogger(WebhookProxy.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MessageBus bus;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response webhook(String payload) {

        LOG.debugf("Received webhook call");

        RequestPayload requestPayload;
        Response.Status status = Response.Status.ACCEPTED;
        try {
            requestPayload = objectMapper.readValue(payload, RequestPayload.class);
            bus.publish(requestPayload);
        } catch (JsonProcessingException e) {
            LOG.error("Error parsing payload", e);
            status = Response.Status.INTERNAL_SERVER_ERROR;
        } catch (IOException e) {
            LOG.error("Error processing in Jenkins", e);
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return Response.status(status).build();

    }

}
