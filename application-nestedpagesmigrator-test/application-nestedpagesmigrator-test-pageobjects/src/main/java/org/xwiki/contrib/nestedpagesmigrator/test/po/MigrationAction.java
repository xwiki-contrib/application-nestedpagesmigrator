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
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * @version $Id: $
 */
public class MigrationAction
{
    private String target;

    private String source;

    private WebElement checkbox;

    private WebElement li;

    private XWikiWebDriver driver;

    public MigrationAction(XWikiWebDriver driver, WebElement li)
    {
        this.li = li;
        this.driver = driver;
        target = li.findElement(By.cssSelector(".targetLocation")).getText();
        source = li.findElement(By.cssSelector(".sourceLocation")).getText();
        checkbox = li.findElement(By.cssSelector("input[type='checkbox']"));
    }

    public String getTarget()
    {
        return target;
    }

    public String getSource()
    {
        return source;
    }

    public boolean isEnabled()
    {
        return checkbox.isSelected();
    }

    public void setEnabled(boolean enabled)
    {
        if (isEnabled() != enabled) {
            checkbox.click();
        }
    }

    public List<MigrationAction> getChildren()
    {
        if (isLeaf()) {
            return Collections.emptyList();
        }

        List<MigrationAction> children = new ArrayList<>();

        openTree();

        for (WebElement child : li.findElements(By.cssSelector("ul.childrenActions > li"))) {
            children.add(new MigrationAction(driver, child));
        }

        return children;
    }

    public List<Preference> getPreferences()
    {
        if (isLeaf()) {
            return Collections.emptyList();
        }

        List<Preference> preferences = new ArrayList<>();

        openTree();

        for (WebElement pref : li.findElements(By.cssSelector("ul.preferences > li"))) {
            preferences.add(new Preference(driver, pref));
        }

        return preferences;
    }

    public boolean isLeaf()
    {
        return li.getAttribute("class").contains("jstree-leaf");
    }

    public boolean isOpen()
    {
        return li.getAttribute("class").contains("jstree-open");
    }

    public void openTree()
    {
        if (isOpen()) {
            return;
        }

        li.findElement(By.tagName("i")).click();

        driver.waitUntilCondition(new ExpectedCondition<Object>()
        {
            @Override
            public Object apply(WebDriver webDriver)
            {
                return isOpen();
            }
        });
    }
}
