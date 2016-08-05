package net.fs.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Poison on 7/11/2016.
 */
public class Command {

    private Command() {

    }

    public static void execute(String command) {

        try {
            Process p = Runtime.getRuntime().exec(command);

            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream())); BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;

                while ((line = stdInput.readLine()) != null) {
                    ConsoleLogger.info(line);
                }
                while ((line = stdError.readLine()) != null) {
                    ConsoleLogger.info(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            ConsoleLogger.error("execute command " + command + " failed!, JVM exit!");
            System.exit(-1);
        }
    }

    public static List<String> executeShellAndGetLines(String command) {

        List<String> lines = new ArrayList<>();

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});

            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream())); BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = stdInput.readLine()) != null) {
                    ConsoleLogger.info(line);
                    lines.add(line);
                }
                while ((line = stdError.readLine()) != null) {
                    ConsoleLogger.info(line);
                    lines.add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            ConsoleLogger.error("execute command " + command + " failed!, JVM exit!");
            System.exit(-1);
        }

        return lines;
    }

}
