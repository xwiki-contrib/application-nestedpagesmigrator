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
package org.xwiki.contrib.nestedpagesmigrator.internal;

import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.script.internal.StatusAndLogSerializer;
import org.xwiki.job.event.status.JobProgress;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogQueue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 * @since 0.4.3
 */
public class StatusAndLogSerializerTest
{
    @Test
    public void serialize() throws Exception
    {
        // Mocks
        JobStatus status = mock(JobStatus.class);
        JobProgress progress = mock(JobProgress.class);
        when(status.getProgress()).thenReturn(progress);
        when(progress.getOffset()).thenReturn(0.42);

        LogQueue logQueue = new LogQueue();
        when(status.getLog()).thenReturn(logQueue);

        logQueue.info("Some INFO message {}", "with a parameter");
        logQueue.debug("Some DEBUG message");
        logQueue.warn("Some WARNING message");
        logQueue.error("Some ERROR message");

        // Test
        String result = StatusAndLogSerializer.serialize(status);

        // Verify
        StringWriter expectedResult = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("/serializedJobStatusAndLogs.json"), expectedResult);

        assertEquals(expectedResult.toString(), result);

    }

    @Test
    public void serializeWithExceptionInLogs() throws Exception
    {
        // Mocks
        JobStatus status = mock(JobStatus.class);
        JobProgress progress = mock(JobProgress.class);
        when(status.getProgress()).thenReturn(progress);
        when(progress.getOffset()).thenReturn(0.42);

        LogQueue logQueue = new LogQueue();
        when(status.getLog()).thenReturn(logQueue);

        logQueue.error("Some ERROR message", new Exception("Terrible Exception"));

        // Test
        String result = StatusAndLogSerializer.serialize(status);

        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(result);
        String strackTrace = rootElement.getAsJsonObject().getAsJsonArray("logs").get(0)
                .getAsJsonObject().getAsJsonPrimitive("stackTrace").getAsString();

        assertTrue(strackTrace.startsWith("java.lang.Exception: Terrible Exception\n" +
                "\tat org.xwiki.contrib.nestedpagesmigrator.internal.StatusAndLogSerializerTest.serializeWithExceptionInLogs"));

    }
}
