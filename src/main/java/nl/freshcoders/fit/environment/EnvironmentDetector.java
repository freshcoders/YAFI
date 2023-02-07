package nl.freshcoders.fit.environment;

/**
 * Fault injection can be dependent on which OS we are running on.
 */
public class EnvironmentDetector {

    public static Environment.OS getOperatingSystem() {
        String os = System.getProperty("os.name");

        if (isMac(os)) {
            return Environment.OS.MAC;
        }
        if (isWindows(os)) {
            return Environment.OS.WINDOWS;
        }
        if (isLinux(os)) {
            return Environment.OS.LINUX;
        }

        return Environment.OS.UNKNOWN;
    }

    private String getTargetOS() {
        // TODO: if target is remote, check remote OS (or settings) instead.
        return System.getProperty("os.name");
    }

    private static boolean isMac(String os) {
        return os.toLowerCase().contains("mac os");
    }

    private static boolean isLinux(String os) {
        return os.toLowerCase().contains("linux");
    }

    private static boolean isWindows(String os) {
        return os.toLowerCase().contains("windows");
    }
}
