package org.horreum.perf.proxy.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
public class PayloadHandler {

    @Inject
    JenkinsServer jenkinsServer;

    @Inject
    JobDefinitionResolver jobs;

    private static final Logger LOG = Logger.getLogger(WebhookProxy.class);

    @WithSpan("requests")
    public void handle(RequestPayload payload) throws IOException {
        if (payload != null) {
            LOG.debugf("Received payload: %s", payload);
        }

        if (!isRunning()) {
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
        JobWithDetails jenkinsJob = getJobWithDetails(jobPath);

        if (jenkinsJob == null) return;

        //start job running
        if (jenkinsJob == null) {
            Log.errorf("Could not retrieve Jenkins job: %s", jobDefinition.jenkinsJob);
            return;
        }

        QueueReference queueReference = null;
        try {
            LOG.debugf("Building jenkins jobs: %s", jobDefinition.jenkinsJob);
            queueReference = buildJob(payload, jenkinsJob);
        } catch (IOException e) {
            Log.errorv(e, "Could not build Jenkins Job: %s", jobDefinition.jenkinsJob);
            return;
        }

        if (queueReference == null) {
            Log.errorf("Could not queue Jenkins Job: %s", jobDefinition.jenkinsJob);
            return;
        }

        LOG.debugf("Retrieving queued job details: %s", jobDefinition.jenkinsJob);

        QueueItem jenkinsQueueItem = getQueueItem(queueReference);

        LOG.infof("Jenkins job queued: %s", jenkinsQueueItem.getTask().getUrl());

    }

    @WithSpan("jenkins-queue-item")
    public QueueItem getQueueItem(QueueReference queueReference) throws IOException {
        return jenkinsServer.getQueueItem(queueReference);
    }

    @WithSpan("jenkins-build-job")
    public QueueReference buildJob(RequestPayload payload, JobWithDetails jenkinsJob) throws IOException {
        QueueReference queueReference;
        if (payload.parameters.isEmpty()) {
            queueReference = jenkinsJob.build();
        } else {
            queueReference = jenkinsJob.build(payload.parameters);
        }
        return queueReference;
    }

    @WithSpan("jenkins-is-running")
    public boolean isRunning() {
        return jenkinsServer.isRunning();
    }

    @WithSpan("jenkins-get-job")
    public JobWithDetails getJobWithDetails(String[] jobPath) throws IOException {
        JobWithDetails jenkinsJob = null;

        for (String jobName : jobPath) {
            if (jenkinsJob == null) {
                jenkinsJob = jenkinsServer.getJob(jobName);
            } else {
                if (!jenkinsJob.isBuildable()) {
                    FolderJob folder = jenkinsServer.getFolderJob(jenkinsJob).orNull();
                    if (folder == null) {
                        Log.errorf("Could not retrieve folder: %s", jenkinsJob.getFullName());
                        return null;
                    }
                    jenkinsJob = jenkinsServer.getJob(folder, jobName);
                } else {
                    Log.errorf("Current job is not a folder: %s", jenkinsJob.getFullName());
                    return null;
                }
            }
        }
        return jenkinsJob;
    }
}

