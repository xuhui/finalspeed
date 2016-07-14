// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import net.fs.rudp.Route;
import net.fs.utils.ByteShortConvert;
import net.fs.utils.ConsoleLogger;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.*;
import org.pcap4j.packet.EthernetPacket.EthernetHeader;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.util.MacAddress;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class CapEnv {

    private boolean isClient = false;

    public boolean isClient() {
        return isClient;
    }

    public MacAddress gateway_mac;

    public MacAddress local_mac;

    Inet4Address local_ipv4;

    public PcapHandle sendHandle;

    VDatagramSocket vDatagramSocket;

    String testIp_tcp = "";

    String testIp_udp = "5.5.5.5";

    String selectedInterfaceName = null;

    String selectedInterfaceDes = "";

    PcapNetworkInterface nif;

    private final int COUNT = -1;

    private final int READ_TIMEOUT = 1;

    private final int SNAPLEN = 10 * 1024;

    short listenPort;

    TunManager tcpManager = null;

    public boolean tcpEnable = false;

    public boolean fwSuccess = true;

    private boolean ppp = false;

    public CapEnv(boolean isClient, boolean fwSuccess) {
        this.isClient = isClient;
        this.fwSuccess = fwSuccess;
        tcpManager = new TunManager(this);
    }

    private void processPacket(Packet packet) throws Exception {
        EthernetPacket ethernetPacket = (EthernetPacket) packet;
        EthernetHeader ethernetHeader = ethernetPacket.getHeader();

        IpV4Packet ipV4Packet = null;
        if (ppp) {
            ipV4Packet = getIpV4PacketFromPppoeInEthernetPacket(ethernetPacket);
        } else {
            if (ethernetPacket.getPayload() instanceof IpV4Packet) {
                ipV4Packet = (IpV4Packet) ethernetPacket.getPayload();
            }
        }
        if (ipV4Packet != null) {
            IpV4Header ipV4Header = ipV4Packet.getHeader();
            if (ipV4Packet.getPayload() instanceof TcpPacket) {
                TcpPacket tcpPacket = (TcpPacket) ipV4Packet.getPayload();
                TcpHeader tcpHeader = tcpPacket.getHeader();
                if (isClient) {
                    TCPTun conn = tcpManager.getTcpConnection_Client(ipV4Header.getSrcAddr().getHostAddress(), tcpHeader.getSrcPort().value(), tcpHeader.getDstPort().value());
                    if (conn != null) {
                        conn.process_client(this, packet, ethernetHeader, ipV4Header, tcpPacket, false);
                    }
                } else {
                    TCPTun conn = null;
                    conn = tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(), tcpHeader.getSrcPort().value());
                    if (
                            tcpHeader.getDstPort().value() == listenPort) {
                        if (tcpHeader.getSyn() && !tcpHeader.getAck() && conn == null) {
                            conn = new TCPTun(this, ipV4Header.getSrcAddr(), tcpHeader.getSrcPort().value());
                            tcpManager.addConnection_Server(conn);
                        }
                        conn = tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(), tcpHeader.getSrcPort().value());
                        if (conn != null) {
                            conn.process_server(packet, ethernetHeader, ipV4Header, tcpPacket, true);
                        }
                    }
                }
            } else if (ethernetPacket.getPayload() instanceof IllegalPacket) {
                ConsoleLogger.error("IllegalPacket!!!");
            }
        }

    }

    private PromiscuousMode getMode(PcapNetworkInterface pi) {
        PromiscuousMode mode;
        String string = (pi.getDescription() + ":" + pi.getName()).toLowerCase();
        // todo why?
        if (string.contains("wireless")) {
            mode = PromiscuousMode.NONPROMISCUOUS;
        } else {
            mode = PromiscuousMode.PROMISCUOUS;
        }
        return mode;
    }

    public boolean initInterface() throws Exception {
        boolean success = false;
        detectInterface();
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        ConsoleLogger.info("Network Interface List: ");
        for (PcapNetworkInterface pi : allDevs) {
            String desString = "";
            if (pi.getDescription() != null) {
                desString = pi.getDescription();
            }
            ConsoleLogger.info("  " + desString + "   " + pi.getName());
            if (pi.getName().equals(selectedInterfaceName)
                    && desString.equals(selectedInterfaceDes)) {
                nif = pi;
                //break;
            }
        }
        if (nif != null) {
            String desString = "";
            if (nif.getDescription() != null) {
                desString = nif.getDescription();
            }
            success = true;
            ConsoleLogger.info("Selected Network Interface:\n" + "  " + desString + "   " + nif.getName());
            if (fwSuccess) {
                tcpEnable = true;
            }
        } else {
            tcpEnable = false;
            ConsoleLogger.info("Select Network Interface failed,can't use TCP protocal!\n");
        }
        if (tcpEnable) {
            sendHandle = nif.openLive(SNAPLEN, getMode(nif), READ_TIMEOUT);
//			final PcapHandle handle= nif.openLive(SNAPLEN, getMode(nif), READ_TIMEOUT);

            String filter = "";
            if (!isClient) {
                //服务端
                filter = "tcp dst port " + listenPort;
            } else {
                //客户端
                filter = "tcp";
            }
            sendHandle.setFilter(filter, BpfCompileMode.OPTIMIZE);

            final PacketListener listener = new PacketListener() {
                @Override
                public void gotPacket(Packet packet) {

                    try {
                        if (packet instanceof EthernetPacket) {
                            processPacket(packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            };

            Thread thread = new Thread() {

                public void run() {
                    try {
                        sendHandle.loop(COUNT, listener);
                        PcapStat ps = sendHandle.getStats();
                        sendHandle.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            thread.start();
        }

        if (!isClient) {
            ConsoleLogger.info("FinalSpeed server start success.");
        }
        return success;

    }

    private void detectInterface() {
        List<PcapNetworkInterface> allDevs;
        HashMap<PcapNetworkInterface, PcapHandle> handleTable = new HashMap<>();
        try {
            allDevs = Pcaps.findAllDevs();
        } catch (PcapNativeException e) {
            e.printStackTrace();
            ConsoleLogger.error("Pcaps.findAllDevs() failed!, JVM exit!");
            System.exit(-1);
            return;
        }

        for (final PcapNetworkInterface pi : allDevs)
            try {
                //todo need to optimize snaplen and timeoutMillis
                final PcapHandle handle = pi.openLive(SNAPLEN, getMode(pi), READ_TIMEOUT);
                handleTable.put(pi, handle);
                final PacketListener listener = packet -> {

                    try {
                        if (packet instanceof EthernetPacket) {
                            EthernetPacket ethernetPacket = (EthernetPacket) packet;
                            EthernetHeader ethernetHeader = ethernetPacket.getHeader();

                            // ETHER_TYPE PPP Session Stage
                            if (ethernetHeader.getType().value() == 0xFFFF8864) {
                                CapEnv.this.ppp = true;
                                PacketUtils.ppp = true;
                            }

                            IpV4Packet ipV4Packet = null;
                            IpV4Header ipV4Header = null;

                            if (ppp) {
                                ipV4Packet = getIpV4PacketFromPppoeInEthernetPacket(ethernetPacket);
                            } else {
                                if (ethernetPacket.getPayload() instanceof IpV4Packet) {
                                    ipV4Packet = (IpV4Packet) ethernetPacket.getPayload();
                                }
                            }
                            if (ipV4Packet != null) {
                                ipV4Header = ipV4Packet.getHeader();

                                if (ipV4Header.getSrcAddr().getHostAddress().equals(testIp_tcp)) {
                                    local_mac = ethernetHeader.getDstAddr();
                                    gateway_mac = ethernetHeader.getSrcAddr();
                                    local_ipv4 = ipV4Header.getDstAddr();
                                    selectedInterfaceName = pi.getName();
                                    if (pi.getDescription() != null) {
                                        selectedInterfaceDes = pi.getDescription();
                                    }
                                    //MLog.info("local_mac_tcp1 "+gateway_mac+" gateway_mac "+gateway_mac+" local_ipv4 "+local_ipv4);
                                }
                                String dstHostAddress = ipV4Header.getDstAddr().getHostAddress();
                                if (testIp_tcp.equals(dstHostAddress) || testIp_udp.equals(dstHostAddress)) {
                                    local_mac = ethernetHeader.getSrcAddr();
                                    gateway_mac = ethernetHeader.getDstAddr();
                                    local_ipv4 = ipV4Header.getSrcAddr();
                                    selectedInterfaceName = pi.getName();
                                    if (pi.getDescription() != null) {
                                        selectedInterfaceDes = pi.getDescription();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                };

                new Thread(() -> {
                    try {
                        handle.loop(COUNT, listener);
                        PcapStat ps = handle.getStats();
                        handle.close();
                    } catch (Exception e) {
                        //  e.printStackTrace();
                        //todo log
                    }
                }).start();
            } catch (PcapNativeException e1) {
                //todo log
            }

        detectMac_tcp();

        Iterator<PcapNetworkInterface> it = handleTable.keySet().iterator();
        while (it.hasNext()) {
            PcapNetworkInterface pi = it.next();
            PcapHandle handle = handleTable.get(pi);
            try {
                handle.breakLoop();
            } catch (NotOpenException e) {
                e.printStackTrace();
            }
            //handle.close();//linux下会阻塞
        }
    }

    private IpV4Packet getIpV4PacketFromPppoeInEthernetPacket(EthernetPacket packet_eth) throws IllegalRawDataException {
        IpV4Packet ipV4Packet = null;
        byte[] pppData = packet_eth.getPayload().getRawData();
        // not use pppData[6] and pppData[7]
        if (pppData.length > 8 && pppData[8] == 0x45) {
            byte[] b2 = new byte[2];
            System.arraycopy(pppData, 4, b2, 0, 2);
            short len = ByteShortConvert.toShort(b2, 0);
            int ipLength = toUnsigned(len) - 2;
            byte[] ipData = new byte[ipLength];
            //设置ppp参数
            PacketUtils.pppHead_static[2] = pppData[2];
            PacketUtils.pppHead_static[3] = pppData[3];
            if (ipLength == (pppData.length - 8)) {
                System.arraycopy(pppData, 8, ipData, 0, ipLength);
                ipV4Packet = IpV4Packet.newPacket(ipData, 0, ipData.length);
            } else {
                ConsoleLogger.info("长度不符!");
            }
        }
        return ipV4Packet;
    }

    public void createTcpTun_Client(String dstAddress, short dstPort) throws Exception {
        Inet4Address serverAddress = (Inet4Address) Inet4Address.getByName(dstAddress);
        TCPTun conn = new TCPTun(this, serverAddress, dstPort, local_mac, gateway_mac);
        tcpManager.addConnection_Client(conn);
        boolean success = false;
        for (int i = 0; i < 6; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (conn.preDataReady) {
                success = true;
                break;
            }
        }
        if (success) {
            tcpManager.setDefaultTcpTun(conn);
        } else {
            tcpManager.removeTun(conn);
            tcpManager.setDefaultTcpTun(null);
            throw new Exception("创建隧道失败!");
        }
    }

    private void detectMac_tcp() {
        InetAddress address = null;
        try {
            address = InetAddress.getByName("bing.com");
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
            try {
                address = InetAddress.getByName("163.com");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                try {
                    address = InetAddress.getByName("apple.com");
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (address == null) {
            ConsoleLogger.info("域名解析失败,请检查DNS设置!");
        }
        final int por = 80;
        testIp_tcp = address.getHostAddress();
        for (int i = 0; i < 5; i++) {
            try {
                Route.executor.execute(() -> {
                    try {
                        Socket socket = new Socket(testIp_tcp, por);
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                Thread.sleep(500);
                if (local_mac != null) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setListenPort(short listenPort) {
        this.listenPort = listenPort;
        if (!isClient) {
            ConsoleLogger.info("Listen tcp port: " + listenPort);
        }
    }

    public static int toUnsigned(short s) {
        return s & 0x0FFFF;
    }

}
