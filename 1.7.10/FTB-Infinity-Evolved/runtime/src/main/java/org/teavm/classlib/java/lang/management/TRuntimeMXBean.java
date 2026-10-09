package org.teavm.classlib.java.lang.management;

import java.util.List;

public interface TRuntimeMXBean {
    List<String> getInputArguments();

    String getVmName();

    String getVmVendor();

    String getVmVersion();

    long getUptime();

    long getStartTime();
}
