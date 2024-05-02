package org.horreum.perf.proxy.proxy.jenkins;


import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.io.IOException;
import java.util.Map;

@ApplicationScoped
@Path("/jenkins")
public class JenkinsProxy {

    @Inject
    JenkinsServer jenkinsServer;

    @GET
    @Path("/jobs")
    @Produces("application/json")
    public Map<String, Job> getJobs() throws IOException {
        return jenkinsServer.getJobs();
    }

}
