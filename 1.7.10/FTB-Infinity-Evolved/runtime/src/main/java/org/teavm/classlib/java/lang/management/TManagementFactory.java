package org.teavm.classlib.java.lang.management;

import java.util.Collections;
import java.util.List;

public class TManagementFactory {
    private static final long START = System.currentTimeMillis();

    private TManagementFactory() {
    }

    public static TRuntimeMXBean getRuntimeMXBean() {
        return new Bean();
    }

    private static final class Bean implements TRuntimeMXBean {
        @Override
        public List<String> getInputArguments() {
            return Collections.emptyList();
        }

        @Override
        public String getVmName() {
            return "TeaVM";
        }

        @Override
        public String getVmVendor() {
            return "TeaVM";
        }

        @Override
        public String getVmVersion() {
            return "0.15";
        }

        @Override
        public long getUptime() {
            return System.currentTimeMillis() - START;
        }

        @Override
        public long getStartTime() {
            return START;
        }
    }
}
