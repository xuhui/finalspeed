// Copyright (c) 2015 D1SM.net

// Copyright (c) 2015 D1SM.net

package net.fs.server;

import net.fs.rudp.Route;
import net.fs.utils.Command;
import net.fs.utils.ConsoleLogger;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;

public class FSServer {

    private static short routePort = 150;
    boolean success_firewall_windows = true;
    private Route route_udp, route_tcp;


    public static void main(String[] args) {
        try {
            FSServer fs = new FSServer();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof BindException) {
                ConsoleLogger.println("Udp port already in use.");
            }
            ConsoleLogger.println("Start failed.");
        }
    }

    private FSServer() throws Exception {

        ConsoleLogger.info("FinalSpeed Server Starting...");

        //todo read port from the configuration file and set

        route_udp = new Route(routePort, Route.MODE_SERVER, false, true);
        if (SystemUtils.IS_OS_LINUX) {
            startFirewall_linux();
            setFireWall_linux_udp();
        } else if (SystemUtils.IS_OS_WINDOWS) {
            startFirewall_windows();
        }

        Route.executor.execute(() -> {
            try {
                route_tcp = new Route(routePort, Route.MODE_SERVER, true, true);
                if (SystemUtils.IS_OS_LINUX) {
                    setFireWall_linux_tcp();
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    if (success_firewall_windows) {
                        setFireWall_windows_tcp();
                    } else {
                        System.out.println("启动windows防火墙失败,请先运行防火墙服务.");
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });

    }


    void startFirewall_windows() {

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

    void setFireWall_windows_tcp() {
        cleanRule_windows();
        try {
            if (SystemUtils.IS_OS_WINDOWS_XP || SystemUtils.IS_OS_WINDOWS_2003) {
                String cmd_add1 = "ipseccmd -w REG -p \"tcptun_fs_server\" -r \"Block TCP/" + routePort + "\" -f *+0:" + routePort + ":TCP " + " -n BLOCK -x ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
            } else {
                String cmd_add1 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=out localport=" + routePort + " action=block ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
                String cmd_add2 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=in localport=" + routePort + " action=block ";
                Process p3 = Runtime.getRuntime().exec(cmd_add2, null);
                p3.waitFor();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    void cleanRule_windows() {
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

    private void startFirewall_linux() {
        Command.execute("service iptables start");
    }

    private void setFireWall_linux_udp() {
        cleanUdpTunRule();
        String cmd2 = "iptables -I INPUT -p udp --dport " + routePort + " -j ACCEPT"
                + " -m comment --comment udptun_fs_server";
        Command.execute(cmd2);
    }

    void cleanUdpTunRule() {
        while (true) {
            int row = getRow("udptun_fs_server");
            if (row > 0) {
                // MLog.println("删除行 "+row);
                String cmd = "iptables -D INPUT " + row;
                Command.execute(cmd);
            } else {
                break;
            }
        }
    }

    void setFireWall_linux_tcp() {
        cleanTcpTunRule();
        String cmd2 = "iptables -I INPUT -p tcp --dport " + routePort + " -j DROP"
                + " -m comment --comment tcptun_fs_server ";
        Command.execute(cmd2);

    }

    void cleanTcpTunRule() {
        while (true) {
            int row = getRow("tcptun_fs_server");
            if (row > 0) {
                // MLog.println("删除行 "+row);
                String cmd = "iptables -D INPUT " + row;
                Command.execute(cmd);
            } else {
                break;
            }
        }
    }

    int getRow(String name) {
        int row_delect = -1;
        String cme_list_rule = "iptables -L -n --line-number";
        // String [] cmd={"netsh","advfirewall set allprofiles state on"};
        Thread errorReadThread = null;
        try {
            final Process p = Runtime.getRuntime().exec(cme_list_rule, null);

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
                                // System.out.println("erroraaa "+line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // error();
                            break;
                        }
                    }
                }
            };
            errorReadThread.start();

            InputStream is = p.getInputStream();
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line;
                try {
                    line = localBufferedReader.readLine();
                    // System.out.println("standaaa "+line);
                    if (line == null) {
                        break;
                    } else {
                        if (line.contains(name)) {
                            int index = line.indexOf("   ");
                            if (index > 0) {
                                String n = line.substring(0, index);
                                try {
                                    if (row_delect < 0) {
                                        // System.out.println("standaaabbb
                                        // "+line);
                                        row_delect = Integer.parseInt(n);
                                    }
                                } catch (Exception e) {

                                }
                            }
                        }
                        ;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            errorReadThread.join();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            // error();
        }
        return row_delect;
    }

}
