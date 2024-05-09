package org.horreum.perf.proxy.proxy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.services.HandlerService;

import java.util.List;

@Path("/api")
public class RestApi {

    @Inject
    ProxyManager proxyManager;

    @Inject
    HandlerService handlerService;

    @GET
    @Path("proxy/running")
    public Boolean getProxy(){
        IProxy proxy = proxyManager.getProxy();
        if ( proxy == null )
            return false;
        return proxy.isRunning();
    }

    @GET
    @Path("proxy/requests")
    @Produces("application/json")
    public List<RequestPayload> getRequests(){
        return handlerService.getRequests();
    }
}
