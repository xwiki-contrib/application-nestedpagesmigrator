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

import java.util.Arrays;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.contrib.nestedpagesmigrator.internal.executor.MigrationPlanExecutor;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.IdentityMigrationAction;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class MigrationPlanExecutorTest
{
    @Rule
    public MockitoComponentMockingRule<MigrationPlanExecutor> mocker =
            new MockitoComponentMockingRule<>(MigrationPlanExecutor.class);

    private JobProgressManager progressManager;
    private JobExecutor jobExecutor;
    private Provider<XWikiContext> contextProvider;
    private EntityReferenceSerializer<String> serializer;
    private DocumentAccessBridge documentAccessBridge;

    private XWikiContext context;
    private XWiki xwiki;
    private Job job;

    @Before
    public void setUp() throws Exception
    {
        progressManager = mocker.getInstance(JobProgressManager.class);
        jobExecutor = mocker.getInstance(JobExecutor.class);
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);

        when(serializer.serialize(any(DocumentReference.class))).thenAnswer(new Answer<String>()
        {
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[0].toString();
            }
        });

        job = mock(Job.class);
        when(jobExecutor.execute(anyString(), any(MoveRequest.class))).thenReturn(job);
    }

    @Test
    public void performMigration() throws Exception
    {
        // Create plan
        MigrationPlanTree plan = new MigrationPlanTree();
        MigrationAction action1 = MigrationAction.createInstance(
                new DocumentReference("xwiki", "Dramas", "WebHome"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Dramas"), "WebHome"),
                plan.getTopLevelAction(),
                plan);

        MigrationAction action2 = MigrationAction.createInstance(
                new DocumentReference("xwiki", "Dramas", "DancesWithWolves"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "DancesWithWolves"), "WebHome"),
                action1,
                plan);

        MigrationAction action3 = MigrationAction.createInstance(
                new DocumentReference("xwiki", "Movies", "Rebbecca"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Rebbecca"), "WebHome"),
                action1,
                plan);

        action3.addPreference(new Preference("skin", "xwiki:XWiki.MoviesSkin", null));
        action3.addPreference(new Preference("iconTheme", "silk", null));
        action3.addPreference(new Preference("colorTheme", "MoviesColorTheme", null));

        MigrationAction action4 = MigrationAction.createInstance(
                new DocumentReference("xwiki", "Movies", "Titanic"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Titanic"), "WebHome"),
                action1,
                plan);

        action4.addRight(new Right(new DocumentReference("xwiki", "XWiki", "UserA"), null, "comment", false, null));
        action4.addRight(new Right(null, new DocumentReference("xwiki", "XWiki", "GroupA"), "create", true, null));

        MigrationAction action5 = MigrationAction.createInstance(
                new DocumentReference("xwiki", "Movies", "Titanic3D"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Titanic", "Titanic3D"), "WebHome"),
                action4,
                plan);

        action5.addRight(new Right(new DocumentReference("xwiki", "XWiki", "UserB"), null, "admin", true, null));
        action5.addRight(new Right(null, new DocumentReference("xwiki", "XWiki", "GroupB"), "delete", false, null));
        action5.addRight(new Right(null, new DocumentReference("xwiki", "XWiki", "GroupC"), "delete", false, null));
        action5.addPreference(new Preference("skin", "xwiki:XWiki.Titanic3DSkin", null));
        action5.addPreference(new Preference("colorTheme", "Titanic3DColorTheme", null));

        MigrationAction action6 = IdentityMigrationAction.createInstance(
                new DocumentReference("xwiki", "Main", "WebHome"),
                plan.getTopLevelAction(),
                plan);

        MigrationAction action7 = MigrationAction.createInstance(new DocumentReference("xwiki", "Movies", "StarWars"),
                new DocumentReference("xwiki", Arrays.asList("Movies", "StarWars"), "WebHome"),
                plan.getTopLevelAction(),
                plan);

        // Create mocks
        XWikiDocument docRebbeccaPreferences = mock(XWikiDocument.class);
        when(xwiki.getDocument(
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Rebbecca"), "WebPreferences")),
                eq(context))).thenReturn(docRebbeccaPreferences);
        BaseObject objRebbeccaPreferences = mock(BaseObject.class);
        when(docRebbeccaPreferences.getXObject(eq(new DocumentReference("xwiki", "XWiki", "XWikiPreferences")),
                eq(true), eq(context))).thenReturn(objRebbeccaPreferences);

        XWikiDocument docTitanicPreferences = mock(XWikiDocument.class);
        when(xwiki.getDocument(
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Titanic"), "WebPreferences")),
                eq(context))).thenReturn(docTitanicPreferences);

        BaseObject objTitanicRight1 = mock(BaseObject.class);
        BaseObject objTitanicRight2 = mock(BaseObject.class);
        when(docTitanicPreferences.newXObject(eq(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights")),
                eq(context))).thenReturn(objTitanicRight1, objTitanicRight2);

        XWikiDocument docTitanic3DPreferences = mock(XWikiDocument.class);
        when(xwiki.getDocument(
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Titanic", "Titanic3D"),
                        "WebPreferences")),
                eq(context))).thenReturn(docTitanic3DPreferences);

        BaseObject objTitanic3DRight1 = mock(BaseObject.class);
        BaseObject objTitanic3DRight2 = mock(BaseObject.class);
        when(docTitanic3DPreferences.newXObject(eq(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights")),
                eq(context))).thenReturn(objTitanic3DRight1, objTitanic3DRight2);
        BaseObject objTitanic3DPreferences = mock(BaseObject.class);
        when(docTitanic3DPreferences.getXObject(eq(new DocumentReference("xwiki", "XWiki", "XWikiPreferences")),
                eq(true), eq(context))).thenReturn(objTitanic3DPreferences);

        // Configuration
        MigrationConfiguration configuration = new MigrationConfiguration(new WikiReference("xwiki"));
        configuration.addDisabledAction("xwiki:Movies.Rebbecca_preference_1");
        configuration.addDisabledAction("xwiki:Movies.Titanic3D_right_2");
        configuration.addDisabledAction("xwiki:Movies.StarWars_page");

        // Test
        mocker.getComponentUnderTest().performMigration(plan, configuration);

        // Verify jobs have been executed
        verify(job, times(5)).join();
        // only 5 times because action6 is identity, and action7 is disabled!

        // Verify document are saved
        String message = "Rights and/or preferences set by the Nested Pages Migrator Application.";
        verify(xwiki, times(1)).saveDocument(eq(docRebbeccaPreferences), eq(message), eq(context));
        verify(xwiki, times(1)).saveDocument(eq(docTitanicPreferences), eq(message), eq(context));
        verify(xwiki, times(1)).saveDocument(eq(docTitanic3DPreferences), eq(message), eq(context));

        // Verify preferences have been set
        verify(objRebbeccaPreferences).set(eq("skin"), eq("xwiki:XWiki.MoviesSkin"), eq(context));
        verify(objRebbeccaPreferences).set(eq("colorTheme"), eq("MoviesColorTheme"), eq(context));
        verify(objTitanic3DPreferences).set(eq("skin"), eq("xwiki:XWiki.Titanic3DSkin"), eq(context));
        verify(objTitanic3DPreferences).set(eq("colorTheme"), eq("Titanic3DColorTheme"), eq(context));

        // verify right have been set
        verify(objTitanicRight1).set(eq("users"), eq("xwiki:XWiki.UserA"), eq(context));
        verify(objTitanicRight1).set(eq("levels"), eq("comment"), eq(context));
        verify(objTitanicRight1).set(eq("allow"), eq(0), eq(context));
        verify(objTitanicRight2).set(eq("groups"), eq("xwiki:XWiki.GroupA"), eq(context));
        verify(objTitanicRight2).set(eq("levels"), eq("create"), eq(context));
        verify(objTitanicRight2).set(eq("allow"), eq(1), eq(context));

        verify(objTitanic3DRight1).set(eq("users"), eq("xwiki:XWiki.UserB"), eq(context));
        verify(objTitanic3DRight1).set(eq("levels"), eq("admin"), eq(context));
        verify(objTitanic3DRight1).set(eq("allow"), eq(1), eq(context));
        verify(objTitanic3DRight2).set(eq("groups"), eq("xwiki:XWiki.GroupB"), eq(context));
        verify(objTitanic3DRight2).set(eq("levels"), eq("delete"), eq(context));
        verify(objTitanic3DRight2).set(eq("allow"), eq(0), eq(context));

        // Verify steps have been triggered
        verify(progressManager).pushLevelProgress(eq(7), any(MigrationPlanExecutor.class));
        verify(progressManager, times(7)).startStep(any(MigrationPlanExecutor.class));
        verify(progressManager).popLevelProgress(any(MigrationPlanExecutor.class));

        // Verify parent fields have been updated
        verify(documentAccessBridge).setDocumentParentReference(eq(action1.getTargetDocument()),
                eq(new DocumentReference("xwiki", "Main", "WebHome")));
        verify(documentAccessBridge).setDocumentParentReference(eq(action2.getTargetDocument()),
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas"), "WebHome")));
        verify(documentAccessBridge).setDocumentParentReference(eq(action3.getTargetDocument()),
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas"), "WebHome")));
        verify(documentAccessBridge).setDocumentParentReference(eq(action4.getTargetDocument()),
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas"), "WebHome")));
        verify(documentAccessBridge).setDocumentParentReference(eq(action5.getTargetDocument()),
                eq(new DocumentReference("xwiki", Arrays.asList("Main", "Dramas", "Titanic"), "WebHome")));
        verify(documentAccessBridge, never()).setDocumentParentReference(eq(action6.getTargetDocument()),
                any(DocumentReference.class));

        // Verify disabled actions have not been executed
        verify(objRebbeccaPreferences, never()).set(eq("iconTheme"), any(), eq(context));
        verify(documentAccessBridge, never()).setDocumentParentReference(eq(action7.getTargetDocument()),
                any(DocumentReference.class));
    }

    @Test
    public void performMigrationWithError() throws Exception
    {
        // Plan
        MigrationPlanTree plan = new MigrationPlanTree();
        MigrationAction action = MigrationAction.createInstance(new DocumentReference("xwiki", "Movies", "WebHome"),
                new DocumentReference("xwiki", Arrays.asList("Main", "Movies"), "WebHome"),
                plan.getTopLevelAction(),
                plan);
        action.addPreference(new Preference("somePref", "someValue", null));

        // Mock
        Exception e = new XWikiException(0, 0, "test exception");
        when(xwiki.getDocument(eq(new DocumentReference("xwiki", Arrays.asList("Main", "Movies"), "WebPreferences")),
                eq(context))).thenThrow(e);

        // Test
        mocker.getComponentUnderTest().performMigration(plan, new MigrationConfiguration(new WikiReference("xwiki")));

        // Verify
        verify(progressManager).pushLevelProgress(eq(1), any(MigrationPlanExecutor.class));
        verify(progressManager, times(1)).startStep(any(MigrationPlanExecutor.class));
        verify(progressManager).popLevelProgress(any(MigrationPlanExecutor.class));

        verify(mocker.getMockedLogger()).warn(eq("Failed to perform the migration of [xwiki:Movies.WebHome]."), eq(e));

    }

}
