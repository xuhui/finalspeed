package net.fs.utils;

import org.apache.commons.lang3.SystemUtils;

/**
 * Created by Poison on 2016/7/11.
 */
public class FireWallUtil {

    private FireWallUtil() {

    }

    public static void allowUdpPort(short port) {
        if (SystemUtils.IS_OS_LINUX) {
            Command.execute("service iptables start");
            allowUdpPortOnLinux(port);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            Command.execute("netsh advfirewall set allprofiles state on");
            allowUdpPortOnWindows(port);
        }
    }

    private static void allowUdpPortOnLinux(short port) {
        clearUdpRuleOnLinux();
        Command.execute("iptables -I INPUT -p udp --dport " + port + " -j ACCEPT -m comment --comment black_envelope_udp");
    }

    private static void clearUdpRuleOnLinux() {
        for (String line : Command.executeShellAndGetLines("iptables -L -n --line-numbers | grep black_envelope_udp")) {
            Command.execute("iptables -D INPUT " + getRuleNumberFromLine(line));
        }
    }

    private static int getRuleNumberFromLine(String line) {
        return Integer.valueOf(line.substring(0, line.indexOf(32)));
    }

    private static void allowUdpPortOnWindows(short port) {
        clearUdpRuleOnWindows();
        Command.execute("netsh advfirewall firewall add rule name=black_envelope_udp protocol=udp dir=in localport=" + port + " action=allow");
    }

    private static void clearUdpRuleOnWindows() {
        Command.execute("netsh advfirewall firewall delete rule name=black_envelope_udp");
    }

    public static void blockTcpPort(short port) {
        if (SystemUtils.IS_OS_LINUX) {
            blockTcpPortOnLinux(port);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            blockTcpPortOnWindows(port);
        }
    }

    private static void blockTcpPortOnLinux(short port) {
        clearTcpRuleOnLinux();
        Command.execute("iptables -I INPUT -p tcp --dport " + port + " -j DROP -m comment --comment black_envelope_tcp");
    }

    private static void clearTcpRuleOnLinux() {
        for (String line : Command.executeShellAndGetLines("iptables -L -n --line-numbers | grep black_envelope_tcp")) {
            Command.execute("iptables -D INPUT " + getRuleNumberFromLine(line));
        }
    }

    private static void blockTcpPortOnWindows(short port) {
        clearTcpRuleOnWindows();
        Command.execute("netsh advfirewall firewall add rule name=black_envelope_tcp protocol=tcp dir=out localport=" + port + " action=block");
        Command.execute("netsh advfirewall firewall add rule name=black_envelope_tcp protocol=tcp dir=in localport=" + port + " action=block");
    }

    private static void clearTcpRuleOnWindows() {
        Command.execute("netsh advfirewall firewall delete rule name=black_envelope_tcp");
    }

}
