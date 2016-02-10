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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Example;
import org.xwiki.contrib.nestedpagesmigrator.testframework.Page;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id: $
 */
public class PreferencesMigrationPlanCreatorTest
{
    @Rule
    public MockitoComponentMockingRule<PreferencesMigrationPlanCreator> mocker =
            new MockitoComponentMockingRule<>(PreferencesMigrationPlanCreator.class);

    private Provider<XWikiContext> contextProvider;
    private XWikiContext context;
    private XWiki xwiki;
    
    @Before
    public void setUp() throws Exception
    {
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
    }

    private void setUpExample(Example example) throws Exception
    {
        for (Page page : example.getAllPages()) {
            for (Preference preference : page.getPreferences()) {
                //preference.getName()
            }
        }
    }

    @Test
    public void testBasicExample() throws Exception
    {
        //testExample("/example8.xml");
    }
}
