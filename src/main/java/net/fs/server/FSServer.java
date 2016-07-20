// Copyright (c) 2015 D1SM.net

// Copyright (c) 2015 D1SM.net

package net.fs.server;

import net.fs.rudp.Route;
import net.fs.utils.ConsoleLogger;
import net.fs.utils.FireWallUtil;

public class FSServer {

    private static short routePort = 150;
    private static Route route_udp;
    private static Route route_tcp;

    public static void main(String[] args) {

        ConsoleLogger.info("FinalSpeed Server Starting...");

        route_udp = new Route(routePort, Route.MODE_SERVER, false);
        FireWallUtil.allowUdpPort(routePort);

        Route.executor.execute(() -> {
            route_tcp = new Route(routePort, Route.MODE_SERVER, true);
            FireWallUtil.blockTcpPort(routePort);
        });

    }

}
