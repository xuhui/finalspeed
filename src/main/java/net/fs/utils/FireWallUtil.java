package net.fs.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

/**
 * Created by Poison on 2016/7/11.
 */
public class FireWallUtil {

    private FireWallUtil() {

    }

    private static boolean success_firewall_windows = true;

    public static void openUdpPort(short port) {
        if (SystemUtils.IS_OS_LINUX) {
            Command.execute("service iptables start");
            openUdpPortOnLinux(port);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            startFirewall_windows();
        }
    }

    private static void openUdpPortOnLinux(short port) {
        clearUdpRule();
        Command.execute("iptables -I INPUT -p udp --dport " + port + " -j ACCEPT -m comment --comment black_envelope_udp");
    }

    private static void clearUdpRule() {
        Optional<List<String>> lines = Command.executeShellAndGetLines("iptables -L -n --line-numbers | grep black_envelope_udp");
        if (lines.isPresent()) {
            for (String line : lines.get()) {
                Command.execute("iptables -D INPUT " + getRuleNumberFromLine(line));
            }
        }
    }

    private static void startFirewall_windows() {

        String runFirewall = "netsh advfirewall set allprofiles state on";
        Thread standReadThread = null;
        Thread errorReadThread = null;
        try {
            final Process p = Runtime.getRuntime().exec(runFirewall, null);
            standReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getInputStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                    while (true) {
                        String line;
                        try {
                            line = localBufferedReader.readLine();
                            if (line == null) {
                                break;
                            } else {
                                if (line.contains("Windows")) {
                                    success_firewall_windows = false;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            //error();
                            break;
                        }
                    }
                }
            };
            standReadThread.start();

            errorReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getErrorStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                    while (true) {
                        String line;
                        try {
                            line = localBufferedReader.readLine();
                            if (line == null) {
                                break;
                            } else {
                                System.out.println("error" + line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            //error();
                            break;
                        }
                    }
                }
            };
            errorReadThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            success_firewall_windows = false;
            //error();
        }

        if (standReadThread != null) {
            try {
                standReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (errorReadThread != null) {
            try {
                errorReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static int getRuleNumberFromLine(String line) {
        return Integer.valueOf(line.substring(0, line.indexOf(32)));
    }

    static void setFireWall_windows_tcp(short port) {
        cleanRule_windows();
        try {
            if (SystemUtils.IS_OS_WINDOWS_XP || SystemUtils.IS_OS_WINDOWS_2003) {
                String cmd_add1 = "ipseccmd -w REG -p \"tcptun_fs_server\" -r \"Block TCP/" + port + "\" -f *+0:" + port + ":TCP " + " -n BLOCK -x ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
            } else {
                String cmd_add1 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=out localport=" + port + " action=block ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
                String cmd_add2 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=in localport=" + port + " action=block ";
                Process p3 = Runtime.getRuntime().exec(cmd_add2, null);
                p3.waitFor();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    static void cleanRule_windows() {
        try {
            if (SystemUtils.IS_OS_WINDOWS_XP || SystemUtils.IS_OS_WINDOWS_2003) {
                String cmd_delete = "ipseccmd -p \"tcptun_fs_server\" -w reg -y";
                final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                p1.waitFor();
            } else {
                String cmd_delete = "netsh advfirewall firewall delete rule name=tcptun_fs_server ";
                final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                p1.waitFor();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openTcpPortOnLinux(short port) {
        clearTcpRule();
        Command.execute("iptables -I INPUT -p tcp --dport " + port + " -j DROP -m comment --comment black_envelope_tcp");
    }

    private static void clearTcpRule() {
        Optional<List<String>> lines = Command.executeShellAndGetLines("iptables -L -n --line-numbers | grep black_envelope_tcp");
        if (lines.isPresent()) {
            for (String line : lines.get()) {
                Command.execute("iptables -D INPUT " + getRuleNumberFromLine(line));
            }
        }
    }

    public static void openTcpPort(short port) {
        if (SystemUtils.IS_OS_LINUX) {
            openTcpPortOnLinux(port);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (success_firewall_windows) {
                setFireWall_windows_tcp(port);
            } else {
                System.out.println("启动windows防火墙失败,请先运行防火墙服务.");
            }
        }
    }
}
