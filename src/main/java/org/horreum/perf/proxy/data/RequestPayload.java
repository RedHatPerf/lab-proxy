package org.horreum.perf.proxy.data;

import java.util.Map;
import java.util.Objects;

public class RequestPayload {

    public String uuid;
    public String jobName;

    public Map<String, String> parameters;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestPayload that = (RequestPayload) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(jobName, that.jobName) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, jobName, parameters);
    }

    @Override
    public String toString() {
        return "\nRequestPayload{\n" +
                "   uuid='" + uuid + '\'' + ",\n" +
                "   jobName='" + jobName + '\'' + ",\n" +
                "   parameters=" + parameters + '\n' +
                '}';
    }
}
