/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.nestedpagesmigrator.script;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.NestedPagesMigrator;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @version $Id: $
 */
@Component(roles = StatusAndLogSerializer.class)
public class StatusAndLogSerializer
{
    @Inject
    private NestedPagesMigrator nestedPagesMigrator;

    private class Log
    {
        public String level;

        public String message;

        public String stackTrace;

        public Log(String level, String message, Throwable throwable)
        {
            this.level = level;
            this.message = message;

            if (throwable != null) {
                StringWriter stackTracePrinter = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stackTracePrinter));
                this.stackTrace = stackTracePrinter.toString();
            }
        }
    }

    public String getStatusAndLogs(String wikiId, String action)
    {
        JobStatus status = nestedPagesMigrator.getStatus(wikiId, action);

        HashMap<String, Object> results = new HashMap<>();

        results.put("state", status.getState());
        results.put("progress", status.getProgress().getOffset());
        results.put("logs", getLogs(status));

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder = gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        return gson.toJson(results);
    }

    private List<Log> getLogs(JobStatus status)
    {
        List<Log> results = new ArrayList<>();

        for (LogEvent event : status.getLog().getLogsFrom(LogLevel.INFO)) {
            results.add(new Log(event.getLevel().toString(), event.getFormattedMessage(), event.getThrowable()));
        }

        return results;
    }
}
