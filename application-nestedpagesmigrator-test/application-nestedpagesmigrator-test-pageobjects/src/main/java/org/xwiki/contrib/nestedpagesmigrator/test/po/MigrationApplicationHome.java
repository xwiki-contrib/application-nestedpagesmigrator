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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id: $
 */
public class MigrationApplicationHome extends ViewPage
{
    @FindBy(id = "projectName")
    private WebElement projectNameInput;

    @FindBy(xpath = "//input[@value = 'Create']")
    private WebElement createProjectButton;

    public static MigrationApplicationHome gotoPage()
    {
        getUtil().gotoPage(new DocumentReference("xwiki", "NestedPagesMigration", "WebHome"));
        return new MigrationApplicationHome();
    }

    public List<String> getProjects()
    {
        List<String> results = new ArrayList<>();
        for (WebElement el : getDriver().findElements(By.cssSelector("#xwikicontent > ul > li"))) {
            results.add(el.getText());
        }
        return results;
    }

    public MigrationProjectPage getMigrationPage(String projectName) throws Exception
    {
        for (WebElement el : getDriver().findElements(By.cssSelector("#xwikicontent > ul > li"))) {
            if (projectName.equals(el.getText())) {
                el.findElement(By.tagName("a")).click();
                return new MigrationProjectPage();
            }
        }

        throw new Exception(String.format("Migration project [%s] has not been found.", projectName));
    }

    public MigrationProjectPage createProject(String projectName)
    {
        projectNameInput.clear();
        projectNameInput.sendKeys(projectName);
        createProjectButton.click();
        return new MigrationProjectPage();
    }
}
