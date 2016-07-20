// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.*;
import net.fs.utils.ConsoleLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PortMapProcess {

    UDPInputStream tis;

    UDPOutputStream tos;

    String serverAddress = "";

    int serverPort;

    ConnectionUDP conn;

    MapClient mapClient;

    public Socket srcSocket;

    DataInputStream srcIs = null;
    DataOutputStream srcOs = null;

    boolean closed = false;
    boolean success = false;

    public PortMapProcess(MapClient mapClient, Route route, final Socket srcSocket, String serverAddress2, int serverPort2, String password_proxy_md5,
                          String dstAddress, final int dstPort) {
        this.mapClient = mapClient;
        this.serverAddress = serverAddress2;
        this.serverPort = serverPort2;

        this.srcSocket = srcSocket;

        try {
            srcIs = new DataInputStream(srcSocket.getInputStream());
            srcOs = new DataOutputStream(srcSocket.getOutputStream());
            conn = route.getConnection(serverAddress, serverPort, null);
            tis = conn.uis;
            tos = conn.uos;

            JSONObject requestJson = new JSONObject();
            requestJson.put("dst_address", dstAddress);
            requestJson.put("dstPort", dstPort);
            byte[] requestData = requestJson.toJSONString().getBytes("utf-8");

            tos.write(requestData, 0, requestData.length);


            final Pipe p1 = new Pipe();
            final Pipe p2 = new Pipe();


            byte[] responeData = tis.read2();

            String hs = new String(responeData, "utf-8");
            JSONObject responeJSon = JSONObject.parseObject(hs);
            int code = responeJSon.getIntValue("code");
            String uimessage = "";
            if (code == Constant.code_success) {

                Route.executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        long t = System.currentTimeMillis();
                        p2.setDstPort(dstPort);
                        try {
                            p2.pipe(tis, srcOs, 1024 * 1024 * 1024, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            close();
                            if (p2.getReadedLength() == 0) {
                                //String msg="fs服务连接成功,加速端口"+dstPort+"连接失败1";
                                String msg = "端口" + dstPort + "无返回数据";
                                ConsoleLogger.info(msg);
                            }
                        }
                    }

                });

                Route.executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            p1.pipe(srcIs, tos, 200 * 1024, p2);
                        } catch (Exception e) {
                            //e.printStackTrace();
                        } finally {
                            close();
                        }
                    }

                });
                success = true;
                ConsoleLogger.info("fs服务连接成功");
            } else {
                close();
                ConsoleLogger.info("fs服务连接成功,端口" + dstPort + "连接失败2");
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            ConsoleLogger.error("fs服务连接失败!");
        }

    }

    void close() {
        if (!closed) {
            closed = true;
            if (srcIs != null) {
                try {
                    srcIs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (srcOs != null) {
                try {
                    srcOs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (tos != null) {
                tos.closeStream_Local();
            }
            if (tis != null) {
                tis.closeStream_Local();
            }
            if (conn != null) {
                conn.close_local();
            }
            if (srcSocket != null) {
                try {
                    srcSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
