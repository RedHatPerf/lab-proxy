package org.horreum.perf.proxy.services;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.io.File;

@ConfigRoot(prefix = "proxy.jobs", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JobDefinitionConfig {
    /**
     * Jenkins CI url
     */
    @ConfigItem
    public File jobDefinitionFile;
}
