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
package org.xwiki.contrib.nestedpagesmigrator.script.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.BeginLogEvent;
import org.xwiki.logging.event.EndLogEvent;
import org.xwiki.logging.event.LogEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serialize a Job Status with only some required information and the logs.
 *
 * TODO: replace GsonBuilder with Jackson Object Mapper, which is the API used by XWiki.
 *
 * @version $Id: $
 * @since 0.4.3
 */
public class StatusAndLogSerializer
{
    private static class LogEventSerializer implements JsonSerializer<LogEvent>
    {
        @Override
        public JsonElement serialize(LogEvent src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject log = new JsonObject();

            log.add("level", new JsonPrimitive(src.getLevel().toString()));
            log.add("message", new JsonPrimitive(src.getFormattedMessage().toString()));

            if (src.getThrowable() != null) {
                StringWriter stackTraceWriter = new StringWriter();
                src.getThrowable().printStackTrace(new PrintWriter(stackTraceWriter));
                log.add("stackTrace", new JsonPrimitive(stackTraceWriter.toString()));
            }

            return log;
        }
    }

    public static String serialize(JobStatus status)
    {
        HashMap<String, Object> results = new HashMap<>();

        results.put("state", status.getState());
        results.put("progress", status.getProgress().getOffset());
        results.put("logs", status.getLog().getLogsFrom(LogLevel.INFO));

        LogEventSerializer logEventSerializer = new LogEventSerializer();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder = gsonBuilder.setPrettyPrinting();
        gsonBuilder = gsonBuilder.registerTypeAdapter(LogEvent.class, logEventSerializer);
        gsonBuilder = gsonBuilder.registerTypeAdapter(BeginLogEvent.class, logEventSerializer);
        gsonBuilder = gsonBuilder.registerTypeAdapter(EndLogEvent.class, logEventSerializer);
        Gson gson = gsonBuilder.create();
        return gson.toJson(results);
    }
}
