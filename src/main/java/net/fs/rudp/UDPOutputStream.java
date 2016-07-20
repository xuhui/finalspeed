// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public class UDPOutputStream {
    Sender sender;

    UDPOutputStream(ConnectionUDP conn) {
        this.sender = conn.sender;
    }

    public void write(byte[] data, int offset, int length) throws InterruptedException {
        sender.sendData(data, offset, length);
    }

    public void closeStream_Local() {
        sender.closeStream_Local();
    }

}
