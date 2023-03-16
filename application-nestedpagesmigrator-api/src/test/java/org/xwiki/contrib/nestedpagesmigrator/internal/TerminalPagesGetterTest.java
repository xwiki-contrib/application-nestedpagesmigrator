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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.PagesToTransformGetter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class TerminalPagesGetterTest
{
    @Rule
    public MockitoComponentMockingRule<PagesToTransformGetter> mocker =
            new MockitoComponentMockingRule<>(PagesToTransformGetter.class);

    private QueryManager queryManager;
    private DocumentReferenceResolver<String> documentReferenceResolver;
    private Provider<XWikiContext> contextProvider;
    private EntityReferenceSerializer<String> referenceSerializer;
    private XWikiContext context;
    private XWiki xwiki;
    private Query query;

    private QueryFilter uniqueFilter;
    private QueryFilter hiddenFilter;

    private final DocumentReference adminRef = new DocumentReference("someWiki", "XWiki", "Admin");

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        referenceSerializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");

        uniqueFilter = mock(QueryFilter.class);
        hiddenFilter = mock(QueryFilter.class);

        mocker.registerComponent(QueryFilter.class, "unique", uniqueFilter);
        mocker.registerComponent(QueryFilter.class, "hidden", hiddenFilter);

        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        query = mock(Query.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);
    }

    @Test
    public void getPagesToConvert() throws Exception
    {
        WikiReference wikiReference = new WikiReference("someWiki");

        // Mocks
        String expectedQuery = "where doc.name <> 'WebPreferences'";
        expectedQuery += " and doc.fullName <> 'XWiki.XWikiPreferences'";
        expectedQuery += " and doc.space in (:includedSpaceList)";
        expectedQuery += " and doc.space not in (:excludedSpaceList)";
        expectedQuery += " and doc.fullName not in (:excludedDocList)";
        expectedQuery += " order by doc.fullName";

        Query query = mock(Query.class);
        when(queryManager.createQuery(eq(expectedQuery), eq(Query.XWQL))).thenReturn(query);

        DocumentReference doc1   = new DocumentReference("someWiki", "someSpace", "excludeMe");
        SpaceReference space1    = new SpaceReference("someWiki", "excludeMe");
        DocumentReference class1 = new DocumentReference("someWiki", "someSpace", "excludeClass");
        SpaceReference space2    = new SpaceReference("someWiki", "someSpace");
        when(referenceSerializer.serialize(doc1)).thenReturn("someSpace.excludeMe");
        when(referenceSerializer.serialize(space1)).thenReturn("excludeMe");
        when(referenceSerializer.serialize(class1)).thenReturn("someWiki:someSpace.excludeClass");
        when(referenceSerializer.serialize(space2)).thenReturn("someSpace");

        when(query.<String>execute()).thenReturn(Arrays.asList("s1.p1", "s1.p2", "s2.p1"));
        DocumentReference r1 = new DocumentReference("someWiki", "s1", "p1");
        DocumentReference r2 = new DocumentReference("someWiki", "s1", "p2");
        DocumentReference r3 = new DocumentReference("someWiki", "s2", "p1");
        when(documentReferenceResolver.resolve("s1.p1", wikiReference)).thenReturn(r1);
        when(documentReferenceResolver.resolve("s1.p2", wikiReference)).thenReturn(r2);
        when(documentReferenceResolver.resolve("s2.p1", wikiReference)).thenReturn(r3);

        XWikiDocument rd1 = mock(XWikiDocument.class);
        XWikiDocument rd2 = mock(XWikiDocument.class);
        XWikiDocument rd3 = mock(XWikiDocument.class);
        when(xwiki.getDocument(r1, context)).thenReturn(rd1);
        when(xwiki.getDocument(r2, context)).thenReturn(rd2);
        when(xwiki.getDocument(r3, context)).thenReturn(rd3);
        when(rd1.getParentReference()).thenReturn(new DocumentReference("someWiki", "Main", "WebHome"));
        when(rd3.getParentReference()).thenReturn(new DocumentReference("someWiki", "Main", "WebHome"));
        when(rd2.getParentReference()).thenReturn(new DocumentReference("someWiki", "Main", "WebHome"));

        // Rd1 is a class
        BaseClass xclass = mock(BaseClass.class);
        when(rd1.getXClass()).thenReturn(xclass);
        Set<String> propertyList = mock(Set.class);
        when(xclass.getPropertyList()).thenReturn(propertyList);
        when(propertyList.isEmpty()).thenReturn(false);

        // Rd2 has a forbidden object
        BaseClass xclass2 = mock(BaseClass.class);
        when(rd2.getXClass()).thenReturn(xclass2);
        when(xclass2.getPropertyList()).thenReturn(new HashSet<String>());
        when(rd2.getXObjectSize(class1)).thenReturn(3);

        // Rd3 is valid!
        BaseClass xclass3 = mock(BaseClass.class);
        when(rd3.getXClass()).thenReturn(xclass3);
        when(xclass3.getPropertyList()).thenReturn(new HashSet<String>());
        when(rd3.getXObjectSize(class1)).thenReturn(0);


        // Test
        MigrationConfiguration configuration = new MigrationConfiguration(wikiReference, this.adminRef);
        configuration.addExcludedPage(new DocumentReference("someWiki", "someSpace", "excludeMe"));
        configuration.addExcludedSpace(new SpaceReference("someWiki", "excludeMe"));
        configuration.addExcludedObjectClass(new DocumentReference("someWiki", "someSpace", "excludeClass"));
        configuration.addIncludedSpace(new SpaceReference("someWiki", "someSpace"));

        List<DocumentReference> results = mocker.getComponentUnderTest().getPagesToConvert(configuration);

        // Verify
        verify(query).setWiki("someWiki");
        verify(query).bindValue(eq("includedSpaceList"), eq(Arrays.asList("someSpace")));
        verify(query).bindValue(eq("excludedSpaceList"), eq(Arrays.asList("excludeMe")));
        verify(query).bindValue(eq("excludedDocList"), eq(Arrays.asList("someSpace.excludeMe")));
        verify(query).addFilter(uniqueFilter);
        verify(query).addFilter(hiddenFilter);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(r3));
    }

    @Test
    public void getPagesToConvertWithException() throws Exception
    {
        // Mock
        QueryException expectedException = mock(QueryException.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenThrow(expectedException);

        // Test
        MigrationException caughtException = null;
        try {
            mocker.getComponentUnderTest()
                .getPagesToConvert(new MigrationConfiguration(new WikiReference("someWiki"), this.adminRef));
        } catch (MigrationException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to get the list of terminal pages.", caughtException.getMessage());
        assertEquals(expectedException, caughtException.getCause());
    }

    @Test
    public void getPagesToConvertWithDontMoveChildren() throws Exception
    {
        WikiReference wikiReference = new WikiReference("someWiki");

        // Mocks
        String expectedQuery = "where doc.name not in ('WebHome', 'WebPreferences') ";
        expectedQuery += "and doc.fullName <> 'XWiki.XWikiPreferences'";
        expectedQuery += " order by doc.fullName";

        Query query = mock(Query.class);
        when(queryManager.createQuery(eq(expectedQuery), eq(Query.XWQL))).thenReturn(query);

        when(query.<String>execute()).thenReturn(Arrays.asList("s1.p1"));
        DocumentReference r1 = new DocumentReference("someWiki", "s1", "p1");
        when(documentReferenceResolver.resolve("s1.p1", wikiReference)).thenReturn(r1);
        XWikiDocument rd1 = mock(XWikiDocument.class);
        when(xwiki.getDocument(r1, context)).thenReturn(rd1);
        when(rd1.getParentReference()).thenReturn(new DocumentReference("xwiki", "Main", "WebHome"));

        // Test
        MigrationConfiguration configuration = new MigrationConfiguration(wikiReference, this.adminRef);
        configuration.setExcludeHiddenPages(false);
        configuration.setDontMoveChildren(true);
        configuration.setExcludeClassPages(false);

        List<DocumentReference> results = mocker.getComponentUnderTest().getPagesToConvert(configuration);

        // Verify
        verify(query, never()).addFilter(hiddenFilter);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(r1));
    }
}