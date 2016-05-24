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
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.model.reference.DocumentReference;

/**
 * Represents an example of wiki holding pages, rights and preferences, and the expected state at the end of the
 * migration. This example is build by parsing an XML document located in the /resources folder.
 *
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
        String spacePart = fullName.substring(0, index);

        String wiki = "xwiki";

        if (spacePart.contains(":")) {
            int wikiPart = spacePart.indexOf(":");
            wiki = spacePart.substring(0, wikiPart);
            spacePart = spacePart.substring(wikiPart);
        }

        String spaces[] = spacePart.split("\\.");
        List<String> spaceList = new ArrayList<>();
        for (String space : spaces) {
            spaceList.add(space);
        }
        return new DocumentReference(wiki, spaceList, page);
    }

    private void elementToRights(Element right, Page page)
    {
        DocumentReference user  = resolveDocument(right.getChildText("user"));
        DocumentReference group = resolveDocument(right.getChildText("group"));
        boolean value           = "allow".equals(right.getChildText("value"));
        DocumentReference origin = resolveDocument(right.getChildText("origin"));
        if (origin == null) {
            origin = new DocumentReference("WebPreferences", page.getDocumentReference().getLastSpaceReference());
        }
        for (String level : right.getChildText("level").split(",")) {
            page.addRight(new Right(user, group, level, value, origin));
        }
    }
    
    private Page getPageFromElement(Element element) 
    {
        DocumentReference reference = resolveDocument(element.getChild("fullName").getText());
        DocumentReference parent = 
                element.getChild("parent") != null ? resolveDocument(element.getChild("parent").getText()) : null;
        DocumentReference from = 
                element.getChild("from") != null ? resolveDocument(element.getChild("from").getText()) : null;
        boolean isFailedToLoad = "true".equals(element.getAttributeValue("errorOnLoad"));
        DocumentReference duplicateOf =
                element.getChild("duplicateOf") != null ? resolveDocument(element.getChildTextTrim("duplicateOf")) :
                        null;
        boolean deletePrevious = "true".equals(element.getAttributeValue("deletePrevious"));

        Page page = new Page(reference, parent, from, isFailedToLoad, duplicateOf, deletePrevious);

        Element preferences = element.getChild("preferences");
        if (preferences != null) {
            for (Element preference : preferences.getChildren()) {
                String name = preference.getChildText("name");
                String value = preference.getChildText("value");
                DocumentReference origin = resolveDocument(preference.getChildText("origin"));
                if (origin == null) {
                    origin = new DocumentReference("WebPreferences", reference.getLastSpaceReference());
                }
                page.addPreference(new Preference(name, value, origin));
            }
        }

        Element rights = element.getChild("rights");
        if (rights != null) {
            for (Element right : rights.getChildren()) {
                elementToRights(right, page);
            }
        }
        
        return page;
    }
    
    public List<DocumentReference> getConcernedPages(MigrationConfiguration configuration)
    {
        // Must be sync with PagesToTransformGetter
        List<DocumentReference> results = new ArrayList<>();
        for (Page page : getAllPages()) {
            if (configuration.isDontMoveChildren()) {
                if (!"WebHome".equals(page.getDocumentReference().getName()) 
                    || "WebPreferences".equals(page.getDocumentReference().getName())) {
                    results.add(page.getDocumentReference());
                }
            } else {
                if (!"WebPreferences".equals(page.getDocumentReference().getName())
                        && (page.getParent() == null
                            || !page.getDocumentReference().toString().equals(page.getParent().toString() + ".WebHome")
                            || !"WebHome".equals(page.getDocumentReference().getName()))) {
                    results.add(page.getDocumentReference());
                }
            }
        }
        return results;
    }
    
    public List<Page> getAllPages()
    {
        List<Page> results = new ArrayList<>();
        for (Element element : getBeforePages()) {
            results.add(getPageFromElement(element));
        }
        return results;
    }
    
    public List<Page> getAllPagesAfter()
    {
        List<Page> results = new ArrayList<>();
        for (Element element : getAfterPages()) {
            results.add(getPageFromElement(element));
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

    public Collection<Preference> getGlobalPreferences()
    {
        DocumentReference origin = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        Collection<Preference> preferences = new ArrayList<>();
        for (Element preference : getBefore().getChild("preferences").getChildren()) {
            String name = preference.getChildText("name");
            String value = preference.getChildText("value");
            preferences.add(new Preference(name, value, origin));
        }
        return preferences;
    }

    public Collection<Right> getGlobalRights()
    {
        DocumentReference origin = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        Collection<Right> rights = new ArrayList<>();
        for (Element right : getBefore().getChild("rights").getChildren()) {
            DocumentReference user  = resolveDocument(right.getChildText("user"));
            DocumentReference group = resolveDocument(right.getChildText("group"));
            boolean value           = "allow".equals(right.getChildText("value"));
            for (String level : right.getChildText("level").split(",")) {
                rights.add(new Right(user, group, level, value, origin));
            }
        }
        return rights;
    }
}
