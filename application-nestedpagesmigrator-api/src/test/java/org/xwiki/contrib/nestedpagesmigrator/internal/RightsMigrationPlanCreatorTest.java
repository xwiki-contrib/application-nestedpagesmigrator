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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class RightsMigrationPlanCreatorTest extends AbstractMigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<RightsMigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(RightsMigrationPlanCreator.class);

    private DocumentAccessBridge documentAccessBridge;
    private JobProgressManager progressManager;
    
    @Before
    public void setUp() throws Exception
    {
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        progressManager = mocker.getInstance(JobProgressManager.class);
    }


    @Override
    protected MigrationPlanTree setUpExample(Example example) throws Exception
    {
        MigrationPlanTree plan = super.setUpExample(example);

        DocumentReference preferencesClass = new DocumentReference("mywiki", "XWiki", "XWikiPreferences");
        // Add mocks for the expected preferences
        for (Page page : example.getAllPages()) {
            for (Right right: page.getRights()) {
                DocumentReference webPreferences
                        = new DocumentReference("WebPreferences", page.getDocumentReference().getLastSpaceReference());
                when(documentAccessBridge.getProperty(eq(webPreferences), eq(preferencesClass),
                        eq("level"))).thenReturn(right.getLevels());
                when(documentAccessBridge.getProperty(eq(webPreferences), eq(preferencesClass),
                        eq("value"))).thenReturn(right.isAllow());
                when(documentAccessBridge.getProperty(eq(webPreferences), eq(preferencesClass),
                        eq("user"))).thenReturn(right.getUser() != null ? right.getUser().toString() : null);
                when(documentAccessBridge.getProperty(eq(webPreferences), eq(preferencesClass),
                        eq("group"))).thenReturn(right.getGroup() != null ? right.getGroup().toString() : null);
            }
        }
        for (Right right : example.getGlobalRights()) {
            when(documentAccessBridge.getProperty(eq(preferencesClass), eq(preferencesClass), eq("level")))
                    .thenReturn(right.getLevels());
            when(documentAccessBridge.getProperty(eq(preferencesClass), eq(preferencesClass), eq("value")))
                    .thenReturn(right.isAllow());
            when(documentAccessBridge.getProperty(eq(preferencesClass), eq(preferencesClass), eq("user")))
                    .thenReturn(right.getUser() != null ? right.getUser().toString() : null);
            when(documentAccessBridge.getProperty(eq(preferencesClass), eq(preferencesClass), eq("group")))
                    .thenReturn(right.getGroup() != null ? right.getGroup().toString() : null);
        }

        return plan;
    }

    private void verifyRights(Example example, MigrationPlanTree plan) throws Exception
    {

    }

    private void testExample(String exampleName) throws Exception
    {
        // Load example
        Example example = new Example(exampleName);

        // Create mocks
        MigrationPlanTree plan = setUpExample(example);

        // Run the component
        mocker.getComponentUnderTest().convertRights(plan,
                new MigrationConfiguration(new WikiReference("mywiki")));

        // Verify
        verifyRights(example, plan);

        // The plan should be the exact same
        assertEquals(
                String.format("The serialized plan is different.\n %s", MigrationPlanSerializer.serialize(plan)),
                example.getPlan(),
                MigrationPlanSerializer.serialize(plan));
    }

    @Test
    public void testBasicExample() throws Exception
    {
        testExample("/example-rights-1.xml");
    }
}
