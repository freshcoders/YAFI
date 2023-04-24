package nl.freshcoders.fit.injector.Node;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class MetricMonitor {
    public static double sampleCpu() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        return  osBean.getSystemLoadAverage();
    }



}
