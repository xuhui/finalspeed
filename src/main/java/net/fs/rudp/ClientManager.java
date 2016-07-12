// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.utils.ConsoleLogger;

import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


class ClientManager {

    private Map<Integer, ClientControl> clientTable = new ConcurrentHashMap<>();

    private static final int RECEIVE_PING_TIMEOUT = 8_000;

    private static final int SEND_PING_INTERVAL = 1_000;

    private Route route;

    ClientManager(Route route) {
        this.route = route;// todo check if used

        // see http://stackoverflow.com/questions/2423284/java-thread-garbage-collected-or-not
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                scanClientControl();
            }
        }).start();

    }

    private void scanClientControl() {

        long currentTimeMillis = System.currentTimeMillis();

        for (Integer clientId : clientTable.keySet()) {
            ClientControl cc = clientTable.get(clientId);
            if (cc != null) {
                if (currentTimeMillis - cc.getLastReceivePingTime() < RECEIVE_PING_TIMEOUT) {
                    if (currentTimeMillis - cc.getLastSendPingTime() > SEND_PING_INTERVAL) {
                        cc.sendPingMessage();
                    }
                } else {
                    ConsoleLogger.info("超时关闭client " + cc.dstIp.getHostAddress() + ":" + cc.dstPort + " " + new Date());//todo LocalDateTime and format
                    clientTable.remove(clientId);
                    cc.close();
                }
            }
        }

    }

    ClientControl getClientControl(int clientId, InetAddress dstIp, int dstPort) {
        ClientControl clientControl = clientTable.get(clientId);
        if (clientControl == null) { // todo check thread safe
            clientControl = new ClientControl(route, clientId, dstIp, dstPort);
            clientTable.put(clientId, clientControl);
        }
        return clientControl;
    }

}
