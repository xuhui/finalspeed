// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import net.fs.utils.ConsoleLogger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TunManager {

    private Map<String, TCPTun> connTable = new ConcurrentHashMap<>();

    static TunManager tunManager;

    {
        tunManager = this;
    }

    TCPTun defaultTcpTun;

    CapEnv capEnv;

    {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                scan();
            }
        }).start();
    }

    TunManager(CapEnv capEnv) {
        this.capEnv = capEnv;
    }

    private void scan() {
        for (String key : connTable.keySet()) {
            TCPTun tun = connTable.get(key);
            if (tun != null) {
                if (tun.preDataReady) {
                    //无数据超时
                    long t = System.currentTimeMillis() - tun.lastReceiveDataTime;
                    if (t > 6000) {
                        connTable.remove(key);
                        if (capEnv.isClient()) {
                            defaultTcpTun = null;
                            ConsoleLogger.info("tcp隧道超时");
                        }
                    }
                } else {
                    //连接中超时
                    if (System.currentTimeMillis() - tun.createTime > 5000) {
                        connTable.remove(key);
                    }
                }
            }
        }
    }

    void removeTun(TCPTun tun) {
        connTable.remove(tun.key);
    }

    public static TunManager get() {
        return tunManager;
    }

    TCPTun getTcpConnection_Client(String remoteAddress, short remotePort, short localPort) {
        return connTable.get(remoteAddress + ":" + remotePort + ":" + localPort);
    }

    void addConnection_Client(TCPTun conn) {
        String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort + ":" + conn.localPort;
        //MLog.info("addConnection "+key);
        conn.setKey(key);
        //todo put or putIfAbsent
        connTable.putIfAbsent(key, conn);
    }

    TCPTun getTcpConnection_Server(String remoteAddress, short remotePort) {
        return connTable.get(remoteAddress + ":" + remotePort);
    }

    void addConnection_Server(TCPTun conn) {
        String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort;
        //MLog.info("addConnection "+key);
        conn.setKey(key);
        //todo put or putIfAbsent
        connTable.putIfAbsent(key, conn);
    }

    TCPTun getDefaultTcpTun() {
        return defaultTcpTun;
    }

    void setDefaultTcpTun(TCPTun defaultTcpTun) {
        this.defaultTcpTun = defaultTcpTun;
    }

}
