// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.server.MapTunnelProcessor;

import java.net.InetAddress;

public class ConnectionUDP {
    public InetAddress dstIp;
    public int dstPort;
    public Sender sender;
    public Receiver receiver;
    public UDPOutputStream uos;
    public UDPInputStream uis;
    long connetionId;
    Route route;
    int mode;
    private boolean connected = true;
    long lastLiveTime = System.currentTimeMillis();

    int connectId;

    public ClientControl clientControl;

    public boolean localClosed = false, remoteClosed = false, destroied = false;

    public boolean stopnow = false;

    public ConnectionUDP(Route route, InetAddress dstIp, int dstPort, int mode, int connectId, ClientControl clientControl) throws Exception {
        this.clientControl = clientControl;
        this.route = route;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.mode = mode;
        if (mode == Route.MODE_CLIENT) {
            //MLog.info("                 发起连接RUDP "+dstIp+":"+dstPort+" connectId "+connectId);
        } else if (mode == Route.MODE_SERVER) {

            //MLog.info("                 接受连接RUDP "+dstIp+":"+dstPort+" connectId "+connectId);
        }
        this.connectId = connectId;
        try {
            sender = new Sender(this);
            receiver = new Receiver(this);
            uos = new UDPOutputStream(this);
            uis = new UDPInputStream(this);
            if (mode == Route.MODE_SERVER) {
                // TODO: 7/7/2016 need to optimize
                new MapTunnelProcessor().process(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            connected = false;
            this.route.connTable.remove(connectId);
            //#MLog.info("                 连接失败RUDP "+connectId);
            synchronized (this) {
                notifyAll();
            }
            throw e;
        }
        //#MLog.info("                 连接成功RUDP "+connectId);
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public String toString() {
        return new String(dstIp + ":" + dstPort);
    }

    public boolean isConnected() {
        return connected;
    }

    public void close_local() {
        if (!localClosed) {
            localClosed = true;
            if (!stopnow) {
                sender.sendCloseMessage_Conn();
            }
            destroy(false);
        }
    }

    public void close_remote() {
        if (!remoteClosed) {
            remoteClosed = true;
            destroy(false);
        }
    }

    //完全关闭
    public void destroy(boolean force) {
        if (!destroied) {
            if ((localClosed && remoteClosed) || force) {
                destroied = true;
                connected = false;
                uis.closeStream_Local();
                uos.closeStream_Local();
                sender.destroy();
                receiver.destroy();
                route.removeConnection(this);
                clientControl.removeConnection(this);
            }
        }
    }

    void live() {
        lastLiveTime = System.currentTimeMillis();
    }
}
