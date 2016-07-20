// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.annotation.JSONField;

import java.net.ServerSocket;

public class MapRule {

    @JSONField(name = "listen_port")
    int listenPort;
    @JSONField(name = "dst_port")
    int dstPort;

    String name;

    boolean using = false;

    ServerSocket serverSocket;

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
