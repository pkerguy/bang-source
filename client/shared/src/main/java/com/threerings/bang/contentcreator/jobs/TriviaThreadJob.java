package com.threerings.bang.contentcreator.jobs;

import com.threerings.bang.contentcreator.webapi.handlers.CommunicationHandler;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TriviaThreadJob implements org.quartz.Job {

    public TriviaThreadJob(CommunicationHandler handler) {
        _handler = handler;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        System.out.println("Sending trivia packet as a bot command...");


    }
    protected CommunicationHandler _handler;
}
