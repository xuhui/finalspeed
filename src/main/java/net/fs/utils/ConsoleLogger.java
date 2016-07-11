// Copyright (c) 2015 D1SM.net

package net.fs.utils;

// TODO: 7/7/2016 need to optimize to log4j2
public class ConsoleLogger {

    public static void info(String string) {
        System.out.println("INFO: " + string);
    }

    public static void error(String string) {
        System.out.println("ERROR: " + string);
    }

    public static void println(String str) {
        System.out.println(str);
    }

}
