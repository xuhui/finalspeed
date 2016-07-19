// Copyright (c) 2015 D1SM.net

package net.fs.client;

import net.fs.rudp.ClientProcessorInterface;
import net.fs.rudp.Route;
import net.fs.rudp.TrafficEvent;
import net.fs.rudp.Trafficlistener;
import net.fs.utils.ConsoleLogger;
import net.fs.utils.NetStatus;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;

public class MapClient implements Trafficlistener {

    Route route_udp, route_tcp;

    short routePort = 45;

    ClientUII ui;

    String serverAddress = "";

    InetAddress address = null;

    int serverPort = 130;

    NetStatus netStatus;

    long lastTrafficTime;

    int downloadSum = 0;

    int uploadSum = 0;

    Thread clientUISpeedUpdateThread;

    int connNum = 0;

    HashSet<ClientProcessorInterface> processTable = new HashSet<ClientProcessorInterface>();

    Object syn_process = new Object();

    static MapClient mapClient;

    PortMapManager portMapManager;

    static int monPort = 25874;

    String systemName = System.getProperty("os.name").toLowerCase();

    boolean useTcp = true;

    MapClient(ClientUI ui) throws Exception {
        this.ui = ui;
        mapClient = this;
        try {
            final ServerSocket socket = new ServerSocket(monPort);
            new Thread() {
                public void run() {
                    try {
                        socket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(0);
                    }
                }
            }.start();
        } catch (Exception e) {
            //e.printStackTrace();
            System.exit(0);
        }
        try {
            route_tcp = new Route(routePort, Route.MODE_CLIENT, true);
        } catch (Exception e1) {
            //e1.printStackTrace();
            throw e1;
        }
        try {
            route_udp = new Route(routePort, Route.MODE_CLIENT, false);
        } catch (Exception e1) {
            //e1.printStackTrace();
            throw e1;
        }
        netStatus = new NetStatus();

        portMapManager = new PortMapManager(this);

        clientUISpeedUpdateThread = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    updateUISpeed();
                }
            }
        };
        clientUISpeedUpdateThread.start();

        Route.addTrafficlistener(this);

    }

    public static MapClient get() {
        return mapClient;
    }

    private void updateUISpeed() {
        ConsoleLogger.info("connNum: "+connNum+" DownSpeed: "+netStatus.getDownSpeed()+" UpSpeed: "+netStatus.getUpSpeed());
    }

    public void setMapServer(String serverAddress, int serverPort, int remotePort, String passwordMd5, String password_proxy_Md5, boolean direct_cn, boolean tcp,
                             String password) {
        if (this.serverAddress == null
                || !this.serverAddress.equals(serverAddress)
                || this.serverPort != serverPort) {

            if (route_tcp.lastClientControl != null) {
                route_tcp.lastClientControl.close();
            }

            if (route_udp.lastClientControl != null) {
                route_udp.lastClientControl.close();
            }

            cleanRule();
            if (serverAddress != null && !serverAddress.equals("")) {
                setFireWallRule(serverAddress, serverPort);
            }

        }
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        address = null;
        useTcp = tcp;
        resetConnection();
    }


    void setFireWallRule(String serverAddress, int serverPort) {
        String ip;
        try {
            ip = InetAddress.getByName(serverAddress).getHostAddress();
            if (systemName.contains("linux")) {
                String cmd2 = "iptables -t filter -A OUTPUT -d " + ip + " -p tcp --dport " + serverPort + " -j DROP -m comment --comment tcptun_fs ";
                runCommand(cmd2);
            }else if (systemName.contains("windows")) {
                try {
                    if (systemName.contains("xp") || systemName.contains("2003")) {
                        String cmd_add1 = "ipseccmd -w REG -p \"tcptun_fs\" -r \"Block TCP/" + serverPort + "\" -f 0/255.255.255.255=" + ip + "/255.255.255.255:" + serverPort + ":tcp -n BLOCK -x ";
                        final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                        p2.waitFor();
                    } else {
                        String cmd_add1 = "netsh advfirewall firewall add rule name=tcptun_fs protocol=TCP dir=out remoteport=" + serverPort + " remoteip=" + ip + " action=block ";
                        final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                        p2.waitFor();
                        String cmd_add2 = "netsh advfirewall firewall add rule name=tcptun_fs protocol=TCP dir=in remoteport=" + serverPort + " remoteip=" + ip + " action=block ";
                        Process p3 = Runtime.getRuntime().exec(cmd_add2, null);
                        p3.waitFor();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void saveFile(byte[] data, String path) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(data);
        } catch (Exception e) {
            throw e;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    void cleanRule() {
        if (systemName.contains("mac os")) {
            cleanTcpTunRule_osx();
        } else if (systemName.contains("linux")) {
            cleanTcpTunRule_linux();
        } else {
            try {
                if (systemName.contains("xp") || systemName.contains("2003")) {
                    String cmd_delete = "ipseccmd -p \"tcptun_fs\" -w reg -y";
                    final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                    p1.waitFor();
                } else {
                    String cmd_delete = "netsh advfirewall firewall delete rule name=tcptun_fs ";
                    final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                    p1.waitFor();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void cleanTcpTunRule_osx() {
        String cmd2 = "sudo ipfw delete 5050";
        runCommand(cmd2);
    }


    void cleanTcpTunRule_linux() {
        while (true) {
            int row = getRow_linux();
            if (row > 0) {
                //MLog.info("删除行 "+row);
                String cmd = "iptables -D OUTPUT " + row;
                runCommand(cmd);
            } else {
                break;
            }
        }
    }

    int getRow_linux() {
        int row_delect = -1;
        String cme_list_rule = "iptables -L -n --line-number";
        //String [] cmd={"netsh","advfirewall set allprofiles state on"};
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
                                //System.out.info("erroraaa "+line);
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


            InputStream is = p.getInputStream();
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line;
                try {
                    line = localBufferedReader.readLine();
                    //	System.out.info("standaaa "+line);
                    if (line == null) {
                        break;
                    } else {
                        if (line.contains("tcptun_fs")) {
                            int index = line.indexOf("   ");
                            if (index > 0) {
                                String n = line.substring(0, index);
                                try {
                                    if (row_delect < 0) {
                                        //System.out.info("standaaabbb "+line);
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
            //error();
        }
        return row_delect;
    }

    void resetConnection() {
        synchronized (syn_process) {

        }
    }

    public void onProcessClose(ClientProcessorInterface process) {
        synchronized (syn_process) {
            processTable.remove(process);
        }
    }

    public void close() {
        //closeAllProxyRequest();
        //poolManage.close();
        //CSocketPool.closeAll();
    }

    public void trafficDownload(TrafficEvent event) {
        ////#MLog.info("下载 "+event.getTraffic());
        netStatus.addDownload(event.getTraffic());
        lastTrafficTime = System.currentTimeMillis();
        downloadSum += event.getTraffic();
    }

    public void trafficUpload(TrafficEvent event) {
        ////#MLog.info("上传 "+event.getTraffic());
        netStatus.addUpload(event.getTraffic());
        lastTrafficTime = System.currentTimeMillis();
        uploadSum += event.getTraffic();
    }

    static void runCommand(String command) {
        Thread standReadThread = null;
        Thread errorReadThread = null;
        try {
            final Process p = Runtime.getRuntime().exec(command, null);
            standReadThread = new Thread() {
                public void run() {
                    InputStream is = p.getInputStream();
                    BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                    while (true) {
                        String line;
                        try {
                            line = localBufferedReader.readLine();
                            //System.out.info("stand "+line);
                            if (line == null) {
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
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
                                //System.out.info("error "+line);
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
            standReadThread.join();
            errorReadThread.join();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            //error();
        }
    }

    public boolean isUseTcp() {
        return useTcp;
    }

    public void setUseTcp(boolean useTcp) {
        this.useTcp = useTcp;
    }

    public ClientUII getUi() {
        return ui;
    }

    public void setUi(ClientUII ui) {
        this.ui = ui;
    }

}
