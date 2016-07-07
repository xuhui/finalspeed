// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.cap.CapEnv;
import net.fs.cap.VDatagramSocket;
import net.fs.rudp.message.MessageType;
import net.fs.utils.ByteIntConvert;
import net.fs.utils.ConsoleLogger;
import net.fs.utils.MessageCheck;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Route {

    public static final int MODE_SERVER = 2;
    public static final int MODE_CLIENT = 1;

    private int mode = MODE_CLIENT;

    int getMode() {
        return mode;
    }

    //todo need to optimize
    public static ThreadPoolExecutor executor = new ThreadPoolExecutor(100, Integer.MAX_VALUE, 10 * 1_000, TimeUnit.MILLISECONDS, new SynchronousQueue());

    public static int localDownloadSpeed, localUploadSpeed;

    //todo need to optimize
    private static List<Trafficlistener> listenerList = new Vector<Trafficlistener>();

    public Map<Integer, ConnectionUDP> connTable = new HashMap<>();
    public AckListManage delayAckManage;
    public CapEnv capEnv = null;
    public ClientControl lastClientControl;
    public boolean useTcpTun = true;
    Thread mainThread;
    Object syn_ds2Table = new Object();
    Random ran = new Random();
    public int localclientId = Math.abs(ran.nextInt());
    private LinkedBlockingQueue<DatagramPacket> packetBuffer = new LinkedBlockingQueue<>();
    HashSet<Integer> setedTable = new HashSet<>();
    HashSet<Integer> closedTable = new HashSet<>();
    ClientManager clientManager;
    private DatagramSocket ds;

    {
        delayAckManage = new AckListManage();
    }

    public Route(short routePort, int mode, boolean tcp, boolean tcpEnvSuccess) throws Exception {
        this.mode = mode;
        useTcpTun = tcp;
        if (useTcpTun) {
            if (this.mode == MODE_SERVER) {
                //服务端
                VDatagramSocket d = new VDatagramSocket(routePort);
                d.setClient(false);
                capEnv = new CapEnv(false, tcpEnvSuccess);
                capEnv.setListenPort(routePort);
                capEnv.init();
                d.setCapEnv(capEnv);

                ds = d;
            } else {
                //客户端
                VDatagramSocket d = new VDatagramSocket();
                d.setClient(true);
                capEnv = new CapEnv(true, tcpEnvSuccess);
                capEnv.init();
                d.setCapEnv(capEnv);

                ds = d;
            }
        } else {
            if (this.mode == Route.MODE_SERVER) {
                ConsoleLogger.info("Listen udp port: " + routePort);
                ds = new DatagramSocket(routePort);
            } else {
                ds = new DatagramSocket();
            }
        }

        clientManager = new ClientManager(this);

        new Thread(() -> {
            while (true) {
                byte[] b = new byte[1500];
                DatagramPacket dp = new DatagramPacket(b, b.length);
                try {
                    ds.receive(dp);
                    //MLog.println("接收 "+dp.getAddress());
                    packetBuffer.add(dp);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();

        mainThread = new Thread() {
            public void run() {
                while (true) {
                    DatagramPacket dp = null;
                    try {
                        dp = packetBuffer.take();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    if (dp == null) {
                        continue;
                    }
                    byte[] dpData = dp.getData();

                    if (dp.getData().length < 4) {
                        return;
                    }

                    //todo need optimize int to short
                    int sType = MessageCheck.checkSType(dp);

                    //MLog.println("route receive MessageType111#"+sType+" "+dp.getAddress()+":"+dp.getPort());

                    final int connectId = ByteIntConvert.toInt(dpData, 4);
                    int remote_clientId = ByteIntConvert.toInt(dpData, 8);

                    if (closedTable.contains(connectId) && connectId != 0) {
                        //#MLog.println("忽略已关闭连接包 "+connectId);
                        continue;
                    }

                    if (sType == MessageType.sType_PingMessage || sType == MessageType.sType_PingMessage2) {
                        ClientControl clientControl = null;
                        if (Route.this.mode == Route.MODE_SERVER) {
                            clientControl = clientManager.getClientControl(remote_clientId, dp.getAddress(), dp.getPort());
                        } else if (Route.this.mode == Route.MODE_CLIENT) {
                            String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                            int sim_clientId = Math.abs(key.hashCode());
                            clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
                        }
                        clientControl.onReceivePacket(dp);
                    } else {
                        //发起
                        if (Route.this.mode == Route.MODE_CLIENT) {
                            if (!setedTable.contains(remote_clientId)) {
                                String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                                int sim_clientId = Math.abs(key.hashCode());
                                ClientControl clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
                                if (clientControl.getClientId_real() == -1) {
                                    clientControl.setClientId_real(remote_clientId);
                                    //#MLog.println("首次设置clientId "+remote_clientId);
                                } else {
                                    if (clientControl.getClientId_real() != remote_clientId) {
                                        //#MLog.println("服务端重启更新clientId "+sType+" "+clientControl.getClientId_real()+" new: "+remote_clientId);
                                        clientControl.updateClientId(remote_clientId);
                                    }
                                }
                                //#MLog.println("cccccc "+sType+" "+remote_clientId);
                                setedTable.add(remote_clientId);
                            }
                        }


                        //udp connection
                        if (Route.this.mode == Route.MODE_SERVER) {
                            //接收
                            try {
                                getConnection2(dp.getAddress(), dp.getPort(), connectId, remote_clientId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        final ConnectionUDP ds3 = connTable.get(connectId);
                        if (ds3 != null) {
                            final DatagramPacket dp2 = dp;
                            ds3.receiver.onReceivePacket(dp2);
                            if (sType == MessageType.sType_DataMessage) {
                                TrafficEvent event = new TrafficEvent("", ran.nextLong(), dp.getLength(), TrafficEvent.type_downloadTraffic);
                                fireEvent(event);
                            }
                        }

                    }
                }
            }
        };
        mainThread.start();

    }

    public static void addTrafficlistener(Trafficlistener listener) {
        listenerList.add(listener);
    }

    static void fireEvent(TrafficEvent event) {
        for (Trafficlistener listener : listenerList) {
            int type = event.getType();
            if (type == TrafficEvent.type_downloadTraffic) {
                listener.trafficDownload(event);
            } else if (type == TrafficEvent.type_uploadTraffic) {
                listener.trafficUpload(event);
            }
        }
    }


    public void sendPacket(DatagramPacket dp) throws IOException {
        ds.send(dp);
    }

    void removeConnection(ConnectionUDP conn) {
        synchronized (syn_ds2Table) {
            closedTable.add(conn.connectId);
            connTable.remove(conn.connectId);
        }
    }

    //接收连接
    public ConnectionUDP getConnection2(InetAddress dstIp, int dstPort, int connectId, int clientId) throws Exception {
        ConnectionUDP conn = connTable.get(connectId);
        if (conn == null) {
            ClientControl clientControl = clientManager.getClientControl(clientId, dstIp, dstPort);
            conn = new ConnectionUDP(this, dstIp, dstPort, Route.MODE_SERVER, connectId, clientControl);
            synchronized (syn_ds2Table) {
                connTable.put(connectId, conn);
            }
            clientControl.addConnection(conn);
        }
        return conn;
    }

    //发起连接
    public ConnectionUDP getConnection(String address, int dstPort, String password) throws Exception {
        InetAddress dstIp = InetAddress.getByName(address);
        int connectId = Math.abs(ran.nextInt());
        String key = dstIp.getHostAddress() + ":" + dstPort;
        int remote_clientId = Math.abs(key.hashCode());
        ClientControl clientControl = clientManager.getClientControl(remote_clientId, dstIp, dstPort);
        clientControl.setPassword(password);
        ConnectionUDP conn = new ConnectionUDP(this, dstIp, dstPort, 1, connectId, clientControl);
        synchronized (syn_ds2Table) {
            connTable.put(connectId, conn);
        }
        clientControl.addConnection(conn);
        lastClientControl = clientControl;
        return conn;
    }

    public boolean isUseTcpTun() {
        return useTcpTun;
    }

    public void setUseTcpTun(boolean useTcpTun) {
        this.useTcpTun = useTcpTun;
    }

}


