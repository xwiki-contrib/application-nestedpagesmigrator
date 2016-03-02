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
import java.util.Collection;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id: $
 * @since 0.4
 */
public class MyViewPage extends ViewPage
{
    @FindBy(id = "xwikicontent")
    private WebElement xwikicontent;

    public static MyViewPage gotoPage(DocumentReference reference)
    {
        getUtil().gotoPage(reference);
        return new MyViewPage();
    }

    public Collection<String> getChildren()
    {
        Collection<String> results = new ArrayList<>();
        for (WebElement webElement : xwikicontent.findElements(By.tagName("a"))) {
            results.add(webElement.getText());
        }
        return results;
    }
}
