// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public interface Trafficlistener {

    void trafficDownload(TrafficEvent event);

    void trafficUpload(TrafficEvent event);

}
