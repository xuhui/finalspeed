// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSON;
import net.fs.utils.ConsoleLogger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientUI implements ClientUII {


    MapClient mapClient;

    private ClientConfig config = null;

    public static ClientUI ui;

    Exception capException = null;

    String systemName = System.getProperty("os.name");

    public boolean isVisible = true;

    ClientUI() {

        ui = this;
        checkRunAsAdministrator();
        loadConfigFromJson();

        try {
            mapClient = new MapClient(this, true);
        } catch (final Exception e1) {
            e1.printStackTrace();
            capException = e1;
            //System.exit(0);
        }

        mapClient.setUi(this);

        mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getRemotePort(), null, null, config.isDirect_cn(), config.getProtocol().equals("tcp"),
                null);

    }

    // TODO: 7/19/2016 need to optimize
    private void checkRunAsAdministrator() {
        if (systemName.contains("windows")) {
            boolean b;
            File file = new File(System.getenv("WINDIR") + "\\test.file");
            //System.out.info("kkkkkkk "+file.getAbsolutePath());
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            b = file.exists();
            file.delete();

            if (!b) {
                //mainFrame.setVisible(true);
                if (isVisible) {
                    JOptionPane.showMessageDialog(null, "请以管理员身份运行! ");
                }
                ConsoleLogger.error("请以管理员身份运行,否则可能无法正常工作! ");
                System.exit(-1);
            }
        }
    }

    private ClientConfig loadConfigFromJson() {
        try {
            ClientConfig clientConfig = JSON.parseObject(new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir") + File.separator + "config.json")), "UTF-8"), ClientConfig.class);
            return config = clientConfig;
        } catch (IOException e) {
            e.printStackTrace();
            ConsoleLogger.error(e.toString());
            System.exit(-1);
        }
        return null;//cannot be
    }

    @Override
    public void setMessage(String message) {
        ConsoleLogger.info(message);
    }

    @Override
    public boolean updateNode(boolean testSpeed) {
        return true;
    }
}
