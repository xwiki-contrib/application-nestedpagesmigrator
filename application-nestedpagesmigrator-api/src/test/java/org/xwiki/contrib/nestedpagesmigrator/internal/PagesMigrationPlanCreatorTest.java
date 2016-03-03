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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanSerializer;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.PagesMigrationPlanCreator;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.PagesToTransformGetter;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class PagesMigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<PagesMigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(PagesMigrationPlanCreator.class);

    private Provider<XWikiContext> contextProvider;
    private XWikiContext context;
    private XWiki xwiki;
    private PagesToTransformGetter pagesToTransformGetter;
    
    private void assertNotEquals(MigrationPlanTree plan, Object o1, Object o2) throws Exception
    {
        Assert.assertNotEquals(String.format("The same value is present twice in the plan: [%s].\n%s",
                o1, MigrationPlanSerializer.serialize(plan)), o1, o2);
    }

    private void verifyMigrationsActionsAreUnique(MigrationPlanTree plan) throws Exception
    {
        ArrayList<MigrationAction> actions = new ArrayList<>(plan.getActions().values());
        for (int i = 1; i < actions.size(); ++i) {
            MigrationAction action = actions.get(i);
            for (int j = 0; j < i; ++j) {
                MigrationAction otherAction = actions.get(j);
                assertNotEquals(plan, action, otherAction);
                assertNotEquals(plan, action.getSourceDocument(), otherAction.getSourceDocument());
                assertNotEquals(plan, action.getTargetDocument(), otherAction.getTargetDocument());
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
        pagesToTransformGetter = mocker.getInstance(PagesToTransformGetter.class);
        
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(any(DocumentReference.class), eq(context))).thenReturn(document);
    }
    
    private void setUpExample(Example example) throws Exception
    {
        for (Page page : example.getAllPages()) {
            if (page.isFailedToLoad()) {
                XWikiException exception = new XWikiException();
                when(xwiki.getDocument(eq(page.getDocumentReference()), eq(context))).thenThrow(exception);   
            } else {
                XWikiDocument document = mock(XWikiDocument.class);
                when(xwiki.getDocument(eq(page.getDocumentReference()), eq(context))).thenReturn(document);
                when(document.getParentReference()).thenReturn(page.getParent());
            }
            when(xwiki.exists(eq(page.getDocumentReference()), eq(context))).thenReturn(true);
        }
    }
    
    private void verifyPlan(MigrationPlanTree plan, Example example) throws Exception
    {
        for (Page page : example.getAllPagesAfter()) {
            MigrationAction action = plan.getActionAbout(page.getFrom());
            // The action must exist
            assertNotNull(
                    String.format("An action concerning [%s] was expected in the plan.\n%s",
                            page.getFrom(), MigrationPlanSerializer.serialize(plan)),
                    action);
            
            // The action has the expected source
            assertEquals(
                    String.format("The action [%s] should have the source [%s].\n%s", action, page.getFrom(),
                            MigrationPlanSerializer.serialize(plan)),
                    page.getFrom(), action.getSourceDocument());
            
            // The action has the expected target (from the example) 
            assertEquals(
                    String.format("The action [%s] should have the target [%s].\n%s", action, page.getDocumentReference(),
                            MigrationPlanSerializer.serialize(plan)),
                    page.getDocumentReference(), action.getTargetDocument());
        }

        // The plan should be the exact same
        assertEquals(
                String.format("The serialized plan is different.\n %s", MigrationPlanSerializer.serialize(plan)),
                example.getPlan(),
                MigrationPlanSerializer.serialize(plan));
    }
    
    private void testExample(String exampleName) throws Exception
    {
        Example example = new Example(exampleName);
        setUpExample(example);

        MigrationConfiguration migrationConfiguration = new MigrationConfiguration(new WikiReference("xwiki"));
        migrationConfiguration.setDontMoveChildren(example.isDontMoveChildrenEnabled());

        when(pagesToTransformGetter.getPagesToConvert(any(MigrationConfiguration.class)))
                .thenReturn(example.getConcernedPages(migrationConfiguration));
        
        MigrationPlanTree plan = mocker.getComponentUnderTest().computeMigrationPlan(migrationConfiguration);

        verifyMigrationsActionsAreUnique(plan);
        verifyPlan(plan, example);
        
        for (Page page : example.getAllPages()) {
            if (page.isFailedToLoad()) {
                verify(mocker.getMockedLogger()).warn(eq("Failed to open the document [{}]."),
                        eq(page.getDocumentReference()), any(XWikiException.class));
            }
        }
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

    @Test
    public void testWithConflicts() throws Exception
    {
        testExample("/example3.xml");
    }

    @Test
    public void testWithoutChildrenMoveButWithConflicts() throws Exception
    {
        testExample("/example4.xml");
    }

    @Test
    public void testWithOrphan() throws Exception
    {
        testExample("/example5.xml");
    }

    @Test
    public void testWithDocumentImpossibleToLoad() throws Exception
    {
        testExample("/example6.xml");
    }

    @Test
    public void testWithParentInOtherWiki() throws Exception
    {
        testExample("/example7.xml");
    }

    @Test
    public void testWithCycle() throws Exception
    {
        testExample("/example8.xml");
    }
}
