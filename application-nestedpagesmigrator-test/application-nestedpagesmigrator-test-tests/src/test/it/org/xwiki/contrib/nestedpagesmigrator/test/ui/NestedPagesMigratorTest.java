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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MigrationPlan;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MigratorPage;
import org.xwiki.contrib.nestedpagesmigrator.test.po.MyViewPage;
import org.xwiki.contrib.nestedpagesmigrator.test.po.Preference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        // We need to import the XAR manually to keep the pages creator and author
        getUtil().importXar(new File(getClass().
                getResource("/application-nestedpagesmigrator-test-data.xar").toURI()));

        MigratorPage migratorPage = MigratorPage.gotoPage();

        // Detect breakages
        List<String> breakages = migratorPage.detectBreakages();
        assertEquals(3, breakages.size());
        assertEquals("Page xwiki:Comedies.AFishCalledWanda will lose its current parent xwiki:Movies.WebHome because " +
                "its location parent is xwiki:Comedies.WebHome.", breakages.get(0));
        assertEquals("Page xwiki:Comedies.VeryBadTrip will lose its current parent xwiki:Movies.WebHome because " +
                "its location parent is xwiki:Comedies.WebHome.", breakages.get(1));
        assertEquals("Page xwiki:Main.Movies.AFishCalledWanda.WebHome will lose its current parent " +
                "xwiki:Movies.WebHome because its location parent is xwiki:Main.Movies.WebHome.", breakages.get(2));

        // Compute a plan
        migratorPage.computePlan();
        assertFalse(migratorPage.isPlanEmpty());

        // Disable some actions
        MigrationPlan plan = migratorPage.getPlan();
        MigrationAction moviesAction = plan.getActions().get(0).getChildren().get(0);
        MigrationAction veryBadTripAction = moviesAction.getChildren().get(3);
        assertEquals("Main.Movies.VeryBadTrip.WebHome", veryBadTripAction.getTarget());
        assertEquals("Comedies.VeryBadTrip", veryBadTripAction.getSource());
        veryBadTripAction.setEnabled(false);

        MigrationAction wandaAction = moviesAction.getChildren().get(0);
        List<Preference> preferences = wandaAction.getPreferences();
        assertEquals(4, preferences.size());
        preferences.get(3).setEnabled(false);

        // Execute it
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

        // Verify the creator and the author are still good
        Page aFishCalledWanda = getUtil().rest().get(
                new DocumentReference("xwiki", Arrays.asList("Main", "Movies", "AFishCalledWanda"), "WebHome"));
        assertEquals("XWiki.CharlesCrichton", aFishCalledWanda.getCreator());
        assertEquals("XWiki.JohnCleeseAuthor", aFishCalledWanda.getAuthor());

        // Now check the preferences
        getUtil().gotoPage(new DocumentReference("xwiki", Arrays.asList("Main", "Movies", "AFishCalledWanda"),
                "WebPreferences"), "edit", "editor=object");
        List<ObjectEditPane> objects = new ObjectEditPage().getObjectsOfClass("XWiki.XWikiPreferences");
        assertEquals(1, objects.size());
        assertEquals("Panels.Welcome", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_rightPanels")));
        assertEquals("0", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_showLeftPanels")));
        assertEquals("1", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_showRightPanels")));
        // Verify this property has not been changed!
        assertEquals("---", objects.get(0).getFieldValue(By.id("XWiki.XWikiPreferences_0_rightPanelsWidth")));

        // That's all!
    }

}
