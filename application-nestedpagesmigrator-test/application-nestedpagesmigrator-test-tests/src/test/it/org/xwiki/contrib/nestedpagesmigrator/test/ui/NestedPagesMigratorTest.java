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
package org.xwiki.contrib.nestedpagesmigrator.test.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MigratorPage;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MyViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id: $
 * @since 0.4
 */
public class NestedPagesMigratorTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testMigration() throws Exception
    {
        MigratorPage migratorPage = MigratorPage.gotoPage();

        migratorPage.computePlan();
        migratorPage.executePlan();

        // Go to the main page
        MyViewPage page = MyViewPage.gotoPage(new DocumentReference("xwiki", "Main", "WebHome"));
        Collection<String> children = page.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.contains("Main.Movies.WebHome"));

        // // Go to Main.Movies.WebHome
        page = MyViewPage.gotoPage(new DocumentReference("xwiki", Arrays.asList("Main", "Movies"), "WebHome"));
        children = page.getChildren();
        assertEquals(3, children.size());
        assertTrue(children.contains("Main.Movies.AFishCalledWanda.WebHome"));
        assertTrue(children.contains("Main.Movies.DancesWithWolves.WebHome"));
        assertTrue(children.contains("Main.Movies.StarTrek.WebHome"));

        // Now check the preferences
        getUtil().gotoPage(new DocumentReference("xwiki", Arrays.asList("Main", "Movies", "AFishCalledWanda"),
                "WebPreferences"), "edit", "editor=object");
        List<ObjectEditPane> objects = new ObjectEditPage().getObjectsOfClass("XWiki.XWikiPreferences");
        assertEquals(1, objects.size());
        assertEquals("Panels.Welcome", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_rightPanels")));
        assertEquals("0", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_showLeftPanels")));
        assertEquals("1", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_showRightPanels")));
        assertEquals("Large", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_rightPanelsWidth")));

        // That's all!
    }

}
