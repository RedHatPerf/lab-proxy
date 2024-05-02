package org.horreum.perf.proxy.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.horreum.perf.proxy.data.JobDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class JobDefinitionResolver {

    private final Map<String, JobDefinition> jobs= new HashMap<>();


    @ConfigProperty(name = "proxy.jobs.job-definition-file")
    File jobDefinitionFile;

    @Inject
    ObjectMapper objectMapper;
    public void startup(@Observes StartupEvent ev) {

        if ( jobDefinitionFile == null ) {
            Log.errorf("Job definition file not set");
            return;
        }

        try (InputStream jobsIS = Files.newInputStream(jobDefinitionFile.toPath())) {

            if (jobsIS != null) {
                JsonNode jobsJson = objectMapper.readTree(jobsIS);
                if ( jobsJson.isArray() ) {
                    jobsJson.forEach( job -> {
                        JobDefinition jobDefinition = objectMapper.convertValue(job, JobDefinition.class);
                        jobs.put(jobDefinition.name, jobDefinition);
                    });
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public JobDefinition getJob(String name) {
        return jobs.get(name);
    }
}
