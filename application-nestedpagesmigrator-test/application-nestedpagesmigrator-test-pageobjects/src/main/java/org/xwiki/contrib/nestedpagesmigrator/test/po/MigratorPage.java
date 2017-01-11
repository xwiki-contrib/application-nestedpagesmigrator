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
package org.xwiki.contrib.nestedpagesmigrator.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id: $
 * @since 0.4
 */
public class MigratorPage extends ViewPage
{
    private static final String emptyPlanXPath = "//div[contains(@class, 'migration-plan')]" +
            "//div[contains(@class, 'infomessage')]/p[contains(text(), 'There is nothing to do!')]";

    @FindBy(xpath = "//button[contains(text(), 'Compute plan')]")
    private WebElement btComputePlan;

    @FindBy(xpath = "//button[contains(text(), 'Detect breakages')]")
    private WebElement btDetectBreakagePlan;

    @FindBy(xpath = "//button[contains(text(), 'Execute plan')]")
    private WebElement btExecutePlan;

    public static MigratorPage gotoPage()
    {
        getUtil().gotoPage(new DocumentReference("xwiki", "NestedPagesMigration", "WebHome"));
        return new MigratorPage();
    }

    public List<String> detectBreakages()
    {
        btDetectBreakagePlan.click();

        getDriver().waitUntilElementIsVisible(By.id("breakageList"));

        // Quite ugly but sometimes knockout displays a thing for a millisecond before hiding it again and so we cannot
        // rely on the visibility of an element to check if the plan is empty or displayed.
        // By waiting "a bit", we make sure knockout really ends its work.
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            // Don't care
        }

        return getBreakages();
    }

    public void computePlan()
    {
        btComputePlan.click();
        getDriver().waitUntilElementIsVisible(By.className("migration-plan"));
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver webDriver)
            {
                return Boolean.valueOf(isPlanDisplayed() || isPlanEmpty());
            }
        });

        // Quite ugly but sometimes knockout displays a thing for a millisecond before hiding it again and so we cannot
        // rely on the visibility of an element to check if the plan is empty or displayed.
        // By waiting "a bit", we make sure knockout really ends its work.
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            // Don't care
        }
    }

    public boolean isPlanDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(By.id("planTree"))
                && getDriver().findElement(By.id("planTree")).isDisplayed();
    }

    public boolean isPlanEmpty()
    {
        return getDriver().hasElementWithoutWaiting(By.xpath(emptyPlanXPath))
                && getDriver().findElement(By.xpath(emptyPlanXPath)).isDisplayed();
    }

    public void executePlan() throws Exception
    {
        if (!btExecutePlan.isEnabled()) {
            throw new Exception("Cannot execute a plan that have not be computed first!");
        }
        getDriver().makeConfirmDialogSilent(true);
        btExecutePlan.click();
        getDriver().waitUntilElementIsVisible(By.id("planExecuted"));
    }

    public List<String> getBreakages()
    {
        List<String> results = new ArrayList<>();

        for (WebElement element : getDriver().findElements(By.cssSelector("#breakageList > li"))) {
            results.add(element.getText());
        }

        return results;
    }

    public MigrationPlan getPlan()
    {
        return new MigrationPlan(getDriver(), getDriver().findElement( By.cssSelector("#planTree > li > ul")));
    }
}
