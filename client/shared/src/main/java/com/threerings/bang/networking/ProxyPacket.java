package com.threerings.bang.networking;

import java.io.Serializable;

public class ProxyPacket implements Serializable {

    public String URL;
    public String response;
    public String usingClass;

    public ProxyPacket(String url) { // Used for Proxying url connections..
        this.URL = url;
    }
    public ProxyPacket(String url, String response, String usingClass) { // Used for response only
        this.URL = url;
        this.response = response;
        this.usingClass = usingClass;
    }

}
