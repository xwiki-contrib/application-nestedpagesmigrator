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

import java.util.ArrayList;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class MigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<MigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(MigrationPlanCreator.class);

    private Provider<XWikiContext> contextProvider;
    private XWikiContext context;
    private XWiki xwiki;
    private TerminalPagesGetter terminalPagesGetter;

    private void verifyMigrationsActionsAreUnique(MigrationPlanTree plan) throws Exception
    {
        ArrayList<MigrationAction> actions = new ArrayList<>(plan.getActions().values());
        for (int i = 1; i < actions.size(); ++i) {
            MigrationAction action = actions.get(i);
            for (int j = 0; j < i; ++j) {
                MigrationAction otherAction = actions.get(j);
                assertNotEquals(action, otherAction);
                assertNotEquals(action.getSourceDocument(), otherAction.getSourceDocument());
                assertNotEquals(action.getTargetDocument(), otherAction.getTargetDocument());
            }
        }
    }
    
    @Before
    public void setUp() throws Exception
    {
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        terminalPagesGetter = mocker.getInstance(TerminalPagesGetter.class);
    }
    
    private void setUpExample(Example example) throws Exception
    {
        for (Page page : example.getAllPages()) {
            XWikiDocument document = mock(XWikiDocument.class);
            when(xwiki.getDocument(eq(page.getDocumentReference()), eq(context))).thenReturn(document);
            when(document.getParentReference()).thenReturn(page.getParent());
        }
    }
    
    private void verifyPlan(MigrationPlanTree plan, Example example) throws Exception
    {
        for (Page page : example.getAllPagesAfter()) {
            MigrationAction action = plan.getActionAbout(page.getFrom());
            assertNotNull(action);
            assertEquals(page.getFrom(), action.getSourceDocument());
            assertEquals(page.getDocumentReference(), action.getTargetDocument());
        }
        
        MigrationPlanSerializer serializer = new MigrationPlanSerializer();
        assertEquals(example.getPlan(), serializer.serialize(plan));
    }
    
    private void testExample(String exampleName) throws Exception
    {
        Example example = new Example(exampleName);
        setUpExample(example);

        when(terminalPagesGetter.getTerminalPages(any(MigrationConfiguration.class)))
                .thenReturn(example.getTerminalPages());

        MigrationConfiguration migrationConfiguration = new MigrationConfiguration(new WikiReference("xwiki"));
        migrationConfiguration.setDontMoveChildren(example.isDontMoveChildrenEnabled());
        MigrationPlanTree plan = mocker.getComponentUnderTest().computeMigrationPlan(migrationConfiguration);

        verifyMigrationsActionsAreUnique(plan);
        verifyPlan(plan, example);
    }
    
    @Test
    public void testBasicExample() throws Exception
    {
        testExample("/example1.xml");
    }

    @Test
    public void testBasicExampleWithoutChildrenMove() throws Exception
    {
        testExample("/example2.xml");
    }
}
