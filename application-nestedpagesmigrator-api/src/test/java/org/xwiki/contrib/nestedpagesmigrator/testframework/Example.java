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
package org.xwiki.contrib.nestedpagesmigrator.testframework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id: $
 */
public class Example
{
    private Document xmlDocument;
    
    public Example(String exampleName) throws JDOMException, IOException
    {
        xmlDocument = new SAXBuilder().build(getClass().getResourceAsStream(exampleName));
    }
    
    private Element getBefore()
    {
        return xmlDocument.getRootElement().getChildren("before").get(0);
    }

    private Element getAfter()
    {
        return xmlDocument.getRootElement().getChildren("after").get(0);
    }
    
    private List<Element> getBeforePages()
    {
        return getBefore().getChildren("page");
    }

    private List<Element> getAfterPages()
    {
        return getAfter().getChildren("page");
    }
    
    private DocumentReference resolveDocument(String fullName)
    {
        if (StringUtils.isBlank(fullName)) {
            return null;
        }
        // This resolver is very limited, but I don't want to inject the real
        int index = fullName.lastIndexOf(".");
        String page = fullName.substring(index + 1);
        String spaces[] = fullName.substring(0, index).split("\\.");
        List<String> spaceList = new ArrayList<>();
        for (String space : spaces) {
            spaceList.add(space);
        }
        return new DocumentReference("xwiki", spaceList, page);
    }
    
    public List<DocumentReference> getTerminalPages()
    {
        List<DocumentReference> results = new ArrayList<>();
        for (Element element : getBeforePages()) {
            DocumentReference reference = resolveDocument(element.getChild("fullName").getText());
            if (!"WebHome".equals(reference.getName()) && !"WebPreferences".equals(reference.getName())) {
                results.add(reference);
            }
        }
        return results;
    }
    
    public List<Page> getAllPages()
    {
        List<Page> results = new ArrayList<>();
        for (Element element : getBeforePages()) {
            DocumentReference reference = resolveDocument(element.getChild("fullName").getText());
            DocumentReference parent = resolveDocument(element.getChild("parent").getText());
            results.add(new Page(reference, parent));
        }
        return results;
    }
    
    public List<Page> getAllPagesAfter()
    {
        List<Page> results = new ArrayList<>();
        for (Element element : getAfterPages()) {
            DocumentReference reference = resolveDocument(element.getChild("fullName").getText());
            DocumentReference from = resolveDocument(element.getChild("from").getText());
            results.add(new Page(reference, null, from));
        }
        return results;
    }
    
    public String getPlan()
    {
        return xmlDocument.getRootElement().getChildren("plan").get(0).getTextTrim();
    }
    
    public boolean isDontMoveChildrenEnabled()
    {
        return "true".equals(xmlDocument.getRootElement().getAttributeValue("dontMoveChildren"));
    }
}
