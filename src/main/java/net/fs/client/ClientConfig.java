// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientConfig {

    @JSONField(name = "server_address")
    private String serverAddress;

    @JSONField(name = "server_port")
    private int serverPort;

    private int remotePort;

    @JSONField(name = "download_speed")
    private int downloadSpeed;
    @JSONField(name = "upload_speed")
    private int uploadSpeed;

    private boolean direct_cn = true;

    @JSONField(name = "socks5_port")
    private int socks5Port;

    private String remoteAddress;

    private String protocol;

}
