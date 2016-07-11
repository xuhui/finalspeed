// Copyright (c) 2015 D1SM.net

// Copyright (c) 2015 D1SM.net

package net.fs.server;

import net.fs.rudp.Route;
import net.fs.utils.ConsoleLogger;
import net.fs.utils.FireWallUtil;

import java.net.BindException;

public class FSServer {

    private static short routePort = 150;
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
        FireWallUtil.openUdpPort(routePort);

        Route.executor.execute(() -> {
            try {
                route_tcp = new Route(routePort, Route.MODE_SERVER, true, true);
                FireWallUtil.openTcpPort(routePort);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });

    }

}
