// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSON;
import net.fs.rudp.Route;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class PortMapManager {

    MapClient mapClient;

    private HashMap<Integer, MapRule> mapRuleTable = new HashMap<>();

    PortMapManager(MapClient mapClient) {
        this.mapClient = mapClient;
        //listenPort();
        loadMapRule();
    }

    private void loadMapRule() {
        try {
            MapRule mapRule = JSON.parseObject(new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "port_map.json")), "UTF-8"), MapRule.class);
            ServerSocket serverSocket = new ServerSocket(mapRule.getListenPort());
            listen(serverSocket);
            mapRule.serverSocket = serverSocket;
            mapRuleTable.put(mapRule.listenPort, mapRule);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void listen(final ServerSocket serverSocket) {
        Route.executor.execute(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        final Socket socket = serverSocket.accept();
                        Route.executor.execute(new Runnable() {

                            @Override
                            public void run() {
                                int listenPort = serverSocket.getLocalPort();
                                MapRule mapRule = mapRuleTable.get(listenPort);
                                if (mapRule != null) {
                                    Route route = null;
                                    if (mapClient.isUseTcp()) {
                                        route = mapClient.route_tcp;
                                    } else {
                                        route = mapClient.route_udp;
                                    }
                                    PortMapProcess process = new PortMapProcess(mapClient, route, socket, mapClient.serverAddress, mapClient.serverPort, null,
                                            null, mapRule.dstPort);
                                }
                            }

                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
    }

}
