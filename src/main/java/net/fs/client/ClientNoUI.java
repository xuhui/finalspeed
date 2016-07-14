// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.Insets;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import net.fs.rudp.Route;
import net.fs.utils.Tools;
import net.fs.utils.Tools;
import net.miginfocom.swing.MigLayout;

import com.alibaba.fastjson.JSONObject;

public class ClientNoUI implements ClientUII{
	
	MapClient mapClient;
	
	ClientConfig config;
	
	String configFilePath="client_config.json";
	
	ClientNoUI(){
		loadConfig();
		Route.localDownloadSpeed=config.downloadSpeed;
		Route.localUploadSpeed=config.uploadSpeed;
//		mapClient=new MapClient(config.getSocks5Port());
//		mapClient.setUi(this);
//		mapClient.setMapServer(config.getServerAddress(), config.getServerPort(),config.getRemotePort()	,config.getPasswordMd5(),config.getPasswordMd5_Proxy(),config.isDirect_cn());
	}
	
	void openUrl(String url){
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
	}
	
	public void setMessage(String message){
		//MLog.info("状态: "+message);
	}
	
	ClientConfig loadConfig(){
		ClientConfig cfg=new ClientConfig();
		if(!new File(configFilePath).exists()){
			JSONObject json=new JSONObject();
			try {
				saveFile(json.toJSONString().getBytes(), configFilePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			String content=readFileUtf8(configFilePath);
			JSONObject json=JSONObject.parseObject(content);
			cfg.setServerAddress(json.getString("server_address"));
			cfg.setServerPort(json.getIntValue("server_port"));
			cfg.setRemotePort(json.getIntValue("remote_port"));
			if(json.containsKey("direct_cn")){
				cfg.setDirect_cn(json.getBooleanValue("direct_cn"));
			}
			cfg.setDownloadSpeed(json.getIntValue("download_speed"));
			cfg.setUploadSpeed(json.getIntValue("upload_speed"));
			if(json.containsKey("socks5_port")){
				cfg.setSocks5Port(json.getIntValue("socks5_port"));
			}
			config=cfg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cfg;
	}
		
	public static String readFileUtf8(String path) throws Exception{
		String str=null;
		FileInputStream fis=null;
		DataInputStream dis=null;
		try {
			File file=new File(path);

			int length=(int) file.length();
			byte[] data=new byte[length];

			fis=new FileInputStream(file);
			dis=new DataInputStream(fis);
			dis.readFully(data);
			str=new String(data,"utf-8");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(dis!=null){
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return str;
	}
	
	void saveFile(byte[] data,String path) throws Exception{
		FileOutputStream fos=null;
		try {
			fos=new FileOutputStream(path);
			fos.write(data);
		} catch (Exception e) {
			throw e;
		} finally {
			if(fos!=null){
				fos.close();
			}
		}
	}


	@Override
	public boolean updateNode(boolean testSpeed) {
		// TODO Auto-generated method stub
		return false;
	}

}
