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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.text.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class PreferencesMigrationPlanCreatorTest extends AbstractMigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<PreferencesMigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(PreferencesMigrationPlanCreator.class);

    private PreferencesPropertiesGetter preferencesPropertiesGetter;
    private DocumentAccessBridge documentAccessBridge;
    private JobProgressManager progressManager;
    
    @Before
    public void setUp() throws Exception
    {
        preferencesPropertiesGetter = mocker.getInstance(PreferencesPropertiesGetter.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        progressManager = mocker.getInstance(JobProgressManager.class);

        when(preferencesPropertiesGetter.getPreferencesProperties()).thenReturn(
                Arrays.asList("skin", "iconTheme", "showLeftPanels"));
    }

    @Override
    protected MigrationPlanTree setUpExample(Example example) throws Exception
    {
        MigrationPlanTree plan = super.setUpExample(example);

        DocumentReference preferencesClass = new DocumentReference("mywiki", "XWiki", "XWikiPreferences");
        // Add mocks for the expected preferences
        for (Page page : example.getAllPages()) {
            for (Preference preference : page.getPreferences()) {
                DocumentReference webPreferences
                        = new DocumentReference("WebPreferences", page.getDocumentReference().getLastSpaceReference());
                when(documentAccessBridge.getProperty(eq(webPreferences), eq(preferencesClass),
                        eq(preference.getName()))).thenReturn(preference.getValue());
            }
        }
        for (Preference preference : example.getGlobalPreferences()) {
            when(documentAccessBridge.getProperty(eq(preferencesClass), eq(preferencesClass), eq(preference.getName())))
                    .thenReturn(preference.getValue());
        }

        return plan;
    }

    private void verifyPreferences(Example example, MigrationPlanTree plan) throws Exception
    {
        for (Page page : example.getAllPagesAfter()) {
            MigrationAction action = plan.getActionWithTarget(page.getDocumentReference());
            for (Preference preference : page.getPreferences()) {
                boolean found = false;
                for (Preference actionPref : action.getPreferences()) {
                    if (actionPref.getName().equals(preference.getName())) {
                        found = true;
                        assertEquals(String.format("Expected preference [%s = %s] for document [%s] has an incorrect " +
                                "value [%s].\n%s", preference.getName(), preference.getValue(),
                                page.getDocumentReference(), actionPref.getValue(),
                                MigrationPlanSerializer.serialize(plan)),
                                preference.getValue(),
                                actionPref.getValue());
                    }
                }
                assertTrue(String.format("Expected preference [%s] for document [%s] was not found.\n%s",
                        preference.getName(), page.getDocumentReference(), MigrationPlanSerializer.serialize(plan)),
                        found);
            }
            for (Preference actionPref : action.getPreferences()) {
                boolean found = false;
                for (Preference preference : page.getPreferences()) {
                    if (StringUtils.equals(preference.getName(), actionPref.getName())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(String.format("Unexpected preference [%s] has been found for document [%s].\n%s",
                        actionPref.getName(), page.getDocumentReference(), MigrationPlanSerializer.serialize(plan)),
                        found);
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
        mocker.getComponentUnderTest().convertPreferences(plan,
                new MigrationConfiguration(new WikiReference("mywiki")));

        // Verify
        verifyPreferences(example, plan);

        // The plan should be the exact same
        assertEquals(
                String.format("The serialized plan is different.\n %s", MigrationPlanSerializer.serialize(plan)),
                example.getPlan(),
                MigrationPlanSerializer.serialize(plan));
    }

    @Test
    public void testBasicExample() throws Exception
    {
        testExample("/example-preferences-1.xml");
    }

    @Test
    public void testExampleWithNestedPages() throws Exception
    {
        testExample("/example-preferences-2.xml");
    }
}
