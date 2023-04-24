package nl.freshcoders.fit.helper;

import java.io.File;
import java.util.Map;

public class DependencyUtil {

    public static final String BYTEMAN_HOME = System.getenv("BYTEMAN_HOME");

    public static boolean validBytemanInstall() {
        // Get a Map of all environment variables and their values
        Map<String, String> env = System.getenv();
        System.out.println("check for bm");
        System.out.println(BYTEMAN_HOME);
        // Check if the BYTEMAN_HOME environment variable is present
        if (null == BYTEMAN_HOME) {
            System.out.println("BYTEMAN_HOME environment variable is not set.");
            return false;
        }

        // Check if the byteman.jar file exists in the BYTEMAN_HOME directory
        File bytemanJar = new File(BYTEMAN_HOME + "/lib/byteman.jar");
        if (!bytemanJar.exists()) {
            System.out.println("byteman.jar file not found in BYTEMAN_HOME directory.");
            return false;
        }

        // If the BYTEMAN_HOME variable is set and the byteman.jar file exists, return true
        return true;
    }

}
