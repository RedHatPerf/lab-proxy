package org.horreum.perf.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.horreum.perf.proxy.data.JobDefinition;
import org.horreum.perf.proxy.data.RequestPayload;
import org.horreum.perf.proxy.proxy.webhook.WebhookProxy;
import org.horreum.perf.proxy.services.JobDefinitionResolver;
import org.jboss.logging.Logger;

import java.io.IOException;

/*

Handler class is responsible for handling the incoming webhook payload,
resolving the allows jenkins job definitions, validating the payload, and triggering the remote Jenkins job.

 */
@ApplicationScoped
public class Handler {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JenkinsServer jenkinsServer;

    @Inject
    JobDefinitionResolver jobs;

    private static final Logger LOG = Logger.getLogger(WebhookProxy.class);

    public void handle(String payload) {
        try {
            handle(objectMapper.readValue(payload, RequestPayload.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void handle(RequestPayload payload) throws IOException {
        if (payload != null) {
            LOG.debugf("Received payload: %s", payload);
        }

        if (!jenkinsServer.isRunning()) {
            LOG.error("Jenkins server is NOT running");
            return;
        }

        JobDefinition jobDefinition = jobs.getJob(payload.jobName);

        if (jobDefinition == null) {
            Log.errorf("Job not found: %s", payload.jobName);
            return;
        }

        for (String param : jobDefinition.requiredParams) {
            if (!payload.parameters.containsKey(param)) {
                Log.errorf("Missing required parameter: %s", param);
                return;
            }
        }

        String[] jobPath = jobDefinition.jenkinsJob.split("/");

        //get job from jenkins

        LOG.debugf("Locating jenkins jobs: %s", jobDefinition.jenkinsJob);


        //todo: there must be an easier way of traversing the job tree!
        //otherwise we go back to a simple http call
        JobWithDetails jenkinsJob = null;
        for (String jobName : jobPath) {
            if (jenkinsJob == null) {
                jenkinsJob = jenkinsServer.getJob(jobName);
            } else {
                if (!jenkinsJob.isBuildable()) {
                    FolderJob folder = jenkinsServer.getFolderJob(jenkinsJob).orNull();
                    if (folder == null) {
                        Log.errorf("Could not retrieve folder: %s", jenkinsJob.getFullName());
                        return;
                    }
                    jenkinsJob = jenkinsServer.getJob(folder, jobName);
                } else {
                    Log.errorf("Current job is not a folder: %s", jenkinsJob.getFullName());
                    return;
                }
            }
        }

        //start job running
        if (jenkinsJob == null) {
            Log.errorf("Could not retrieve Jenkins job: %s", jobDefinition.jenkinsJob);
            return;
        }

        QueueReference queueReference = null;
        try {
            LOG.debugf("Building jenkins jobs: %s", jobDefinition.jenkinsJob);
            if (payload.parameters.isEmpty()) {
                queueReference = jenkinsJob.build();
            } else {
                queueReference = jenkinsJob.build(payload.parameters);
            }
        } catch (IOException e) {
            Log.errorv(e, "Could not build Jenkins Job: %s", jobDefinition.jenkinsJob);
            return;
        }

        if (queueReference == null) {
            Log.errorf("Could not queue Jenkins Job: %s", jobDefinition.jenkinsJob);
            return;
        }

        LOG.debugf("Retrieving queued job details: %s", jobDefinition.jenkinsJob);

        QueueItem jenkinsQueueItem = jenkinsServer.getQueueItem(queueReference);

        LOG.infof("Jenkins job queued: %s", jenkinsQueueItem.getTask().getUrl());


    }
}

