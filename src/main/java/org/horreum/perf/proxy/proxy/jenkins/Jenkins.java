package org.horreum.perf.proxy.proxy.jenkins;

import com.offbytwo.jenkins.JenkinsServer;
import io.quarkus.logging.Log;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URISyntaxException;

public class Jenkins {


    @ConfigProperty(name = "proxy.jenkins.url")
    String url;

    @ConfigProperty(name = "proxy.jenkins.user")
    String user;

    @ConfigProperty(name = "proxy.jenkins.apiKey")
    String apiKey;

    @Produces
    public JenkinsServer jenkinsServer() throws URISyntaxException {
        if ( url == null || user == null || apiKey == null) {
            Log.errorf("Incorrect configuration for Jenkins instance");
            return null;
        }

        JenkinsServer server = new JenkinsServer(new URI(url), user, apiKey);

        return server;

    }
}
