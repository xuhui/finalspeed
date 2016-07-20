// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.utils.ConsoleLogger;

public class UDPInputStream {

    Receiver receiver;

    boolean streamClosed = false;

    ConnectionUDP conn;

    UDPInputStream(ConnectionUDP conn) {
        this.conn = conn;
        receiver = conn.receiver;
    }

    public int read(byte[] b, int off, int len) throws InterruptedException {
        byte[] b2 = read2();
        if (len < b2.length) {
            ConsoleLogger.error("error5");
            System.exit(-1);
            //todo need to optimize
            return 0;
        } else {
            System.arraycopy(b2, 0, b, off, b2.length);
            return b2.length;
        }
    }

    public byte[] read2() throws InterruptedException {
        return receiver.receive();
    }

    public void closeStream_Local() {
        if (!streamClosed) {
            receiver.closeStream_Local();
        }
    }


}
