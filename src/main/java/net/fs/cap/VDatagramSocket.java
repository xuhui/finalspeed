// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import net.fs.rudp.Route;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class VDatagramSocket extends DatagramSocket {

    private boolean isClient = true;

    private LinkedBlockingQueue<TunData> packetQueue = new LinkedBlockingQueue<>();

    private CapEnv capEnv;

    private Object syn_tun = new Object();

    private boolean tunConnecting = false;

    public VDatagramSocket() throws SocketException {

    }

    public void send(DatagramPacket p) throws IOException {
        TCPTun tun = null;
        if (isClient) {
            tun = capEnv.tcpManager.getDefaultTcpTun();
            if (tun != null) {
                if (!tun.remoteAddress.getHostAddress().equals(p.getAddress().getHostAddress())
                        || CapEnv.toUnsigned(tun.remotePort) != p.getPort()) {
                    capEnv.tcpManager.removeTun(tun);
                    capEnv.tcpManager.setDefaultTcpTun(null);
                }
            } else {
                tryConnectTun_Client(p.getAddress(), (short) p.getPort());
                tun = capEnv.tcpManager.getDefaultTcpTun();
            }
        } else {
            tun = capEnv.tcpManager.getTcpConnection_Server(p.getAddress().getHostAddress(), (short) p.getPort());
        }
        if (tun != null) {
            if (tun.preDataReady) {
                tun.sendData(p.getData());
            } else {
                throw new IOException("隧道未连接!");
            }
        } else {

            throw new IOException("隧道不存在! " + " thread " + Route.executor.getActiveCount() + " " + p.getAddress() + ":" + p.getPort());
        }
    }


    private void tryConnectTun_Client(InetAddress dstAddress, short dstPort) {
        synchronized (syn_tun) {
            if (capEnv.tcpManager.getDefaultTcpTun() == null) {
                if (tunConnecting) {
                    try {
                        syn_tun.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    tunConnecting = true;
                    try {
                        capEnv.createTcpTun_Client(dstAddress.getHostAddress(), dstPort);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    tunConnecting = false;
                }
            }
        }
    }

    public synchronized void receive(DatagramPacket p) throws IOException {
        TunData td = null;
        try {
            td = packetQueue.take();
            p.setData(td.data);
            p.setLength(td.data.length);
            p.setAddress(td.tun.remoteAddress);
            p.setPort(CapEnv.toUnsigned(td.tun.remotePort));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void onReceinveFromTun(TunData td) {
        packetQueue.add(td);
    }

    public boolean isClient() {
        return isClient;
    }

    public void setClient(boolean client) {
        this.isClient = client;
    }

    public CapEnv getCapEnv() {
        return capEnv;
    }

    public void setCapEnv(CapEnv capEnv) {
        this.capEnv = capEnv;
        capEnv.vDatagramSocket = this;
    }

}
