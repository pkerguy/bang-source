package com.threerings.bang.contentcreator.webapi.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.contentcreator.jobs.TriviaThreadJob;
import com.threerings.bang.contentcreator.webapi.Server;
import com.threerings.bang.util.BangContext;
import com.threerings.presents.client.InvocationService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class CommunicationHandler implements HttpHandler {

    public CommunicationHandler(BangContext ctx) throws SchedulerException {
        _ctx = ctx;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, String> params = Server.processQuery(httpExchange.getRequestURI().getQuery());
        String response = "Nope.avi";
        if(params.containsKey("key") && params.containsKey("d"))
        {
            if(!params.get("key").equalsIgnoreCase("4UGVCIYVUIVYT756878UGVU"))
            {
                response = "Invalid data recieved! Error #104";
                httpExchange.sendResponseHeaders(200, response.length());
                OutputStream os = httpExchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            if(params.get("d").equalsIgnoreCase("isOK")) {
                if (!_ctx.getUserObject().isActive()) {
                    try {
                        if(scheduler.isStarted())
                        {
                            scheduler.shutdown(false); // Force shutdown this.. They somehow got it to run improperly
                        }
                    } catch (SchedulerException ignore) {}
                    response = "NOTACTIVE";
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                } else if (!_ctx.getUserObject().getTokens().isContentCreator()) // This API only works if they are a content creator
                {
                    try {
                        if(scheduler.isStarted())
                        {
                            scheduler.shutdown(false); // Force shutdown this.. They somehow got it to run improperly
                        }
                    } catch (SchedulerException ignore) {}
                    response = "ACCESSDENIED";
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                } else {
                    try {
                        if(!scheduler.isStarted())
                        {
                            // Trivia Job
                            JobDetail mainTriviaThread = newJob(TriviaThreadJob.class)
                                    .withIdentity("triviaThread", "MAIN")
                                    .build();
                            Trigger mainTriviaTrigger = newTrigger()
                                    .withIdentity("triviaTrigger", "MAIN")
                                    .startNow()
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInMinutes(1) // Run every minute??
                                            .repeatForever())
                                    .build();
                            scheduler.scheduleJob(mainTriviaThread, mainTriviaTrigger); // Now lets finish this job

                            // End of Trivia Job

                            // Now let's start the scheduler
                            scheduler.start();
                        }
                        response = "GRANT";
                        httpExchange.sendResponseHeaders(200, response.length());
                        OutputStream os = httpExchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    } catch (SchedulerException e) {
                        response = "SCHEDULER_ERROR";
                        httpExchange.sendResponseHeaders(200, response.length());
                        OutputStream os = httpExchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                }
            }
            // The base data packet is temp! // TODO: REMOVE BASE DATA PACKET
            if(params.get("d").equalsIgnoreCase("base"))
            {
                if (!_ctx.getUserObject().isActive()) {
                    response = "NOTACTIVE";
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                } else if (!_ctx.getUserObject().getTokens().isContentCreator()) // This API only works if they are a content creator
                {
                    response = "ACCESSDENIED";
                    httpExchange.sendResponseHeaders(200, response.length());
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                } else {

                }
            }
            if(params.get("d").equalsIgnoreCase("gameStatus"))
            {
                _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                    "gameStatus",
                    new InvocationService.ConfirmListener() {

                        @Override
                        public void requestFailed(String s) {
                            try {
                                httpExchange.sendResponseHeaders(200, s.length());
                                OutputStream os = httpExchange.getResponseBody();
                                os.write(s.getBytes());
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }

                        @Override
                        public void requestProcessed() {
                            try {
                                httpExchange.sendResponseHeaders(200, "UNABLE".length());
                                OutputStream os = httpExchange.getResponseBody();
                                os.write("UNABLE".getBytes());
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    });
            }
            if(params.get("d").contains("pollresult_"))
            {
                String[] cmd = params.get("d").split("_");
                System.out.println(cmd[1]);
                _ctx.getClient().requireService(PlayerService.class).serverTunnel(
                        "pollresult:" + cmd[1],
                        new InvocationService.ConfirmListener() {

                            @Override
                            public void requestFailed(String s) {
                                try {
                                    httpExchange.sendResponseHeaders(200, s.length());
                                    OutputStream os = httpExchange.getResponseBody();
                                    os.write(s.getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return;
                            }

                            @Override
                            public void requestProcessed() {
                                try {
                                    httpExchange.sendResponseHeaders(200, "UNABLE".length());
                                    OutputStream os = httpExchange.getResponseBody();
                                    os.write("UNABLE".getBytes());
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return;
                            }
                        });
            }
        }
    }

    protected BangContext _ctx;
    protected Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
    public static ArrayList<String> sendCmd = new ArrayList<String>(); // Our remote commands
    public static boolean recievedCommand = false;
}
