package org.horreum.perf.proxy.proxy.jenkins;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "proxy.jenkins", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JenkinsConfig {

    /**
     * Jenkins CI url
     */
    @ConfigItem
    public String url;

    /**
     * jenkins User
     */
    @ConfigItem
    public String user;


    /**
     * jenkins API key to authenticate user
     */
    @ConfigItem
    public String apikey;

}
