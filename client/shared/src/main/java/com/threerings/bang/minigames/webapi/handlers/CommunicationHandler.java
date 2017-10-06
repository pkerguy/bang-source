package com.threerings.bang.minigames.webapi.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.threerings.bang.minigames.webapi.Server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class CommunicationHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, String> params = Server.processQuery(httpExchange.getRequestURI().getQuery());
        String response = "Nope.avi";
        if(params.containsKey("d"))
        {

            response = "Data returns: " + params.get("d");
        }
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
