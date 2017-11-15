package com.threerings.bang.contentcreator.webapi;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.threerings.bang.client.BangClient;
import com.threerings.bang.contentcreator.webapi.handlers.CommunicationHandler;
import com.threerings.bang.contentcreator.webapi.handlers.MainHandler;
import com.threerings.bang.contentcreator.webapi.handlers.TestHandler;
import com.threerings.bang.util.BangContext;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static HttpServer server;

    public static boolean init(BangContext ctx)
    {
        _client = ctx;
        try {
            server = HttpServer.create(new InetSocketAddress(8100), 0);
            server.createContext("/", new MainHandler());
            server.createContext("/test", new TestHandler());
            try {
                server.createContext("/communicate", new CommunicationHandler(ctx));
            } catch (SchedulerException e) {
                System.out.println("Failed to register main communications api");
            }
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

    private static BangContext _client;
}
