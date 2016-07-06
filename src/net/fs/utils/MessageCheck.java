// Copyright (c) 2015 D1SM.net

package net.fs.utils;

import java.net.DatagramPacket;

public class MessageCheck {

    public static int checkVer(DatagramPacket dp) {
        return ByteShortConvert.toShort(dp.getData(), 0);
    }

    public static int checkSType(DatagramPacket dp) {
        return ByteShortConvert.toShort(dp.getData(), 2);
    }

}
