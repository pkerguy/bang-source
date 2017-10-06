package com.threerings.bang.minigames.webapi;

import com.sun.net.httpserver.HttpsServer;
import com.threerings.bang.minigames.webapi.handlers.CommunicationHandler;
import com.threerings.bang.minigames.webapi.handlers.MainHandler;
import com.threerings.bang.minigames.webapi.handlers.TestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static HttpsServer server;

    public static boolean init()
    {
        try {
            server = HttpsServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/", new MainHandler());
            server.createContext("/test", new TestHandler());
            server.createContext("/communicate", new CommunicationHandler());
            server.start(); // Finally we can start the web server
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean shutdown()
    {
        server.stop(0);
        return true;
    }
    public static Map<String, String> processQuery(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
