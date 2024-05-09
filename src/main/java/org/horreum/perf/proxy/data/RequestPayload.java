package org.horreum.perf.proxy.data;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class RequestPayload {

    @Nullable
    public Instant timestamp;
    public String jobName;
    public Map<String, String> parameters;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestPayload that = (RequestPayload) o;
        return Objects.equals(jobName, that.jobName) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, parameters);
    }

    @Override
    public String toString() {
        return "\nRequestPayload{\n" +
                "   jobName='" + jobName + '\'' + ",\n" +
                "   parameters=" + parameters + '\n' +
                '}';
    }
}
