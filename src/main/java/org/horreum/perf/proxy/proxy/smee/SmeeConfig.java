package org.horreum.perf.proxy.proxy.smee;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for the Smee proxy
 */
@ConfigRoot(prefix = "proxy.smee", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SmeeConfig {

    /**
     * Jenkins CI url
     */
    @ConfigItem
    public String uid;

}
