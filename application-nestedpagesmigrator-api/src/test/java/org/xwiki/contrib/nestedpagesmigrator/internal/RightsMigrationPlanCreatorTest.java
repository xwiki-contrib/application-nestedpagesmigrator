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
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanSerializer;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.contrib.nestedpagesmigrator.internal.rights.DocumentRightsBridge;
import org.xwiki.contrib.nestedpagesmigrator.internal.rights.RightsMigrationPlanCreator;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.contrib.nestedpagesmigrator.testframework.PlanCreator;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class RightsMigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<RightsMigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(RightsMigrationPlanCreator.class);

    private DocumentRightsBridge documentRightsBridge;
    private JobProgressManager progressManager;
    
    @Before
    public void setUp() throws Exception
    {
        documentRightsBridge = mocker.getInstance(DocumentRightsBridge.class);
        progressManager = mocker.getInstance(JobProgressManager.class);
    }


    protected MigrationPlanTree setUpExample(Example example) throws Exception
    {
        MigrationPlanTree plan = PlanCreator.createPlan(example);

        // Add mocks for the current preferences
        for (Page page : example.getAllPages()) {
            // In our example framework, only 'WebHome' can have global preferences
            // (--> bound to WebPreferences actually)
            if (page.getDocumentReference().getName().equals("WebHome")) {
                DocumentReference webPreferences
                        = new DocumentReference("WebPreferences", page.getDocumentReference().getLastSpaceReference());
                when(documentRightsBridge.getRights(eq(webPreferences))).thenReturn(page.getRights());
                for (Right right : page.getRights()) {
                    assertEquals(webPreferences, right.getOrigin());
                }
            }
        }

        DocumentReference globalPreferences = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        when(documentRightsBridge.getRights(eq(globalPreferences))).thenReturn(example.getGlobalRights());
        for (Right right : example.getGlobalRights()) {
            assertEquals(globalPreferences, right.getOrigin());
        }

        return plan;
    }

    private void verifyRights(Example example, MigrationPlanTree plan) throws Exception
    {
        String serializedPlan = MigrationPlanSerializer.serialize(plan);

        for (Page page : example.getAllPagesAfter()) {
            MigrationAction action = plan.getActionWithTarget(page.getDocumentReference());
            assertNotNull(String.format("An action with the target [%s] was expected.\n%s",
                    page.getDocumentReference(), serializedPlan ), action);
            for (Right expectedRight : page.getRights()) {
                boolean found = false;
                for (Right right : action.getRights()) {
                    if (right.equals(expectedRight)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(String.format("%s was expected for document [%s].\n%s", expectedRight.toString(),
                        page.getDocumentReference(), serializedPlan ), found);
            }

            // The other way
            for (Right right : action.getRights()) {
                boolean found = false;
                for (Right expectedRight : page.getRights()) {
                    if (expectedRight.equals(right)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(String.format("Unexpected %s found on document [%s].\n%s", right.toString(),
                        page.getDocumentReference(), serializedPlan), found);
            }
        }
    }

    private void testExample(String exampleName) throws Exception
    {
        // Load example
        Example example = new Example(exampleName);

        // Create mocks
        MigrationPlanTree plan = setUpExample(example);

        // Run the component
        mocker.getComponentUnderTest().convertRights(plan);

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
