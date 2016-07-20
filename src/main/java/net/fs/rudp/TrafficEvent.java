// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public class TrafficEvent {

    long eventId;

    int traffic;

    public static int type_downloadTraffic = 10;

    public static int type_uploadTraffic = 11;

    int type = type_downloadTraffic;

    String userId;

    public TrafficEvent(String userId, long eventId, int traffic, int type) {
        this.userId = userId;
        this.eventId = eventId;
        this.traffic = traffic;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public int getTraffic() {
        return traffic;
    }

}
