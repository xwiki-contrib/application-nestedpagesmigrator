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
import java.util.Collection;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.contrib.nestedpagesmigrator.internal.rights.DocumentRightsBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class DocumentRightsBridgeTest
{
    @Rule
    public MockitoComponentMockingRule<DocumentRightsBridge> mocker =
            new MockitoComponentMockingRule<>(DocumentRightsBridge.class);

    private Provider<XWikiContext> contextProvider;
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private XWikiContext context;
    private XWiki xwiki;

    private DocumentReference group1 = new DocumentReference("someWiki", "XWiki", "group1");
    private DocumentReference group2 = new DocumentReference("someWiki", "XWiki", "group2");
    private DocumentReference group3 = new DocumentReference("someWiki", "XWiki", "group3");
    private DocumentReference user1 = new DocumentReference("someWiki", "XWiki", "user1");
    private DocumentReference user2 = new DocumentReference("someWiki", "XWiki", "user2");
    private DocumentReference user3 = new DocumentReference("someWiki", "XWiki", "user3");
    private DocumentReference user4 = new DocumentReference("someWiki", "XWiki", "user4");

    @Before
    public void setUp() throws Exception
    {
        documentReferenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        WikiReference wikiReference = new WikiReference("someWiki");

        when(documentReferenceResolver.resolve(eq("group1"), eq(wikiReference))).thenReturn(group1);
        when(documentReferenceResolver.resolve(eq("group2"), eq(wikiReference))).thenReturn(group2);
        when(documentReferenceResolver.resolve(eq("group3"), eq(wikiReference))).thenReturn(group3);
        when(documentReferenceResolver.resolve(eq("user1"), eq(wikiReference))).thenReturn(user1);
        when(documentReferenceResolver.resolve(eq("user2"), eq(wikiReference))).thenReturn(user2);
        when(documentReferenceResolver.resolve(eq("user3"), eq(wikiReference))).thenReturn(user3);
        when(documentReferenceResolver.resolve(eq("user4"), eq(wikiReference))).thenReturn(user4);
    }

    @Test
    public void getRights() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("someWiki", "someSpace", "WebPreferences");

        // Mocks
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentReference), eq(context))).thenReturn(document);

        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);
        BaseObject obj3 = mock(BaseObject.class);

        when(document.getXObjects(eq(new DocumentReference("someWiki", "XWiki", "XWikiGlobalRights"))))
                .thenReturn(Arrays.asList(obj1, null, obj2, obj3));

        when(obj1.getLargeStringValue("groups")).thenReturn("group1,group2,,group3");
        when(obj1.getLargeStringValue("levels")).thenReturn("create,comment,,edit");
        when(obj1.getIntValue("allow", 1)).thenReturn(1);

        when(obj2.getLargeStringValue("users")).thenReturn("user1,,user2,user3,");
        when(obj2.getLargeStringValue("levels")).thenReturn("create,comment,,edit");
        when(obj2.getIntValue("allow", 1)).thenReturn(0);

        when(obj3.getLargeStringValue("users")).thenReturn("user4");
        when(obj3.getLargeStringValue("levels")).thenReturn("admin");
        when(obj3.getIntValue("allow", 1)).thenReturn(0);

        // Test
        Collection<Right> results = mocker.getComponentUnderTest().getRights(documentReference);

        // Verify
        assertNotNull(results);
        assertEquals(19, results.size());
        assertTrue(results.contains(new Right(null, group1, "create", true, documentReference)));
        assertTrue(results.contains(new Right(null, group1, "comment", true, documentReference)));
        assertTrue(results.contains(new Right(null, group1, "edit", true, documentReference)));
        assertTrue(results.contains(new Right(null, group2, "create", true, documentReference)));
        assertTrue(results.contains(new Right(null, group2, "comment", true, documentReference)));
        assertTrue(results.contains(new Right(null, group2, "edit", true, documentReference)));
        assertTrue(results.contains(new Right(null, group3, "create", true, documentReference)));
        assertTrue(results.contains(new Right(null, group3, "comment", true, documentReference)));
        assertTrue(results.contains(new Right(null, group3, "edit", true, documentReference)));

        assertTrue(results.contains(new Right(user1, null, "create", false, documentReference)));
        assertTrue(results.contains(new Right(user1, null, "comment", false, documentReference)));
        assertTrue(results.contains(new Right(user1, null, "edit", false, documentReference)));
        assertTrue(results.contains(new Right(user2, null, "create", false, documentReference)));
        assertTrue(results.contains(new Right(user2, null, "comment", false, documentReference)));
        assertTrue(results.contains(new Right(user2, null, "edit", false, documentReference)));
        assertTrue(results.contains(new Right(user3, null, "create", false, documentReference)));
        assertTrue(results.contains(new Right(user3, null, "comment", false, documentReference)));
        assertTrue(results.contains(new Right(user3, null, "edit", false, documentReference)));

        assertTrue(results.contains(new Right(user4, null, "admin", false, documentReference)));
    }

    @Test
    public void getRightsWithException() throws Exception
    {
        // Mocks
        XWikiException expectedException = new XWikiException();
        when(xwiki.getDocument(any(DocumentReference.class), eq(context))).thenThrow(expectedException);

        // Test
        MigrationException caughtException = null;
        try {
            mocker.getComponentUnderTest().getRights(new DocumentReference("someWiki", "someSpace", "WebPreferences"));
        } catch (MigrationException e) {
            caughtException = e;
        }

        assertNotNull(caughtException);
        assertEquals("Failed to get the objects of the document [someWiki:someSpace.WebPreferences].",
                caughtException.getMessage());
        assertEquals(expectedException, caughtException.getCause());

    }
}
