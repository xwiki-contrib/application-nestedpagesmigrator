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
package org.xwiki.contrib.nestedpagesmigrator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * The different options that a the user can fill to perform a migration.
 *
 * @version $Id: $
 */
public class MigrationConfiguration implements Serializable
{
    private boolean excludeHiddenPages;
    
    private boolean excludeClassPages;
    
    private boolean dontMoveChildren;
    
    private boolean addAutoRedirect;
    
    private List<SpaceReference> excludedSpaces = new ArrayList<>();
    
    private List<SpaceReference> includedSpaces = new ArrayList<>();

    private List<DocumentReference> excludedObjectClasses = new ArrayList<>();

    private List<DocumentReference> excludedPages = new ArrayList<>();

    private Set<String> disabledActions = new HashSet<>();
    
    private WikiReference wikiReference;

    /**
     * Create a new configuration.
     * @param wikiReference the reference of the wiki where the migration will occur
     */
    public MigrationConfiguration(WikiReference wikiReference)
    {
        excludeHiddenPages = true;
        excludeClassPages = true;
        dontMoveChildren = false;
        addAutoRedirect = true;
        this.wikiReference = wikiReference;
    }

    /**
     * @return if the configuration has included spaces
     */
    public boolean hasIncludedSpaces()
    {
        return !includedSpaces.isEmpty();
    }

    /**
     * @return if the configuration has excluded spaces
     */
    public boolean hasExcludedSpaces()
    {
        return !excludedSpaces.isEmpty();
    }

    /**
     * @return if the configuration has excluded pages
     */
    public boolean hasExcludedPages()
    {
        return !excludedPages.isEmpty();
    }

    /**
     * @return if the migration should ignore hidden pages
     */
    public boolean isExcludeHiddenPages()
    {
        return excludeHiddenPages;
    }

    /**
     * @param excludeHiddenPages if the migration should ignore hidden pages
     */
    public void setExcludeHiddenPages(boolean excludeHiddenPages)
    {
        this.excludeHiddenPages = excludeHiddenPages;
    }

    /**
     * @return if the migration should ignore pages holding a class
     */
    public boolean isExcludeClassPages()
    {
        return excludeClassPages;
    }

    /**
     * @param excludeClassPages if the migration should ignore pages holding a class
     */
    public void setExcludeClassPages(boolean excludeClassPages)
    {
        this.excludeClassPages = excludeClassPages;
    }

    /**
     * @return if the migration should not move the pages under their parents but only transform them to nested pages.
     */
    public boolean isDontMoveChildren()
    {
        return dontMoveChildren;
    }

    /**
     * @param dontMoveChildren if the migration should not move the pages under their parents but only transform them to
     * nested pages.
     */
    public void setDontMoveChildren(boolean dontMoveChildren)
    {
        this.dontMoveChildren = dontMoveChildren;
    }

    public List<SpaceReference> getExcludedSpaces()
    {
        return new ArrayList<>(excludedSpaces);
    }

    public List<SpaceReference> getIncludedSpaces()
    {
        return new ArrayList<>(includedSpaces);
    }

    public List<DocumentReference> getExcludedObjectClasses()
    {
        return new ArrayList<>(excludedObjectClasses);
    }

    public List<DocumentReference> getExcludedPages()
    {
        return excludedPages;
    }

    public void addExcludedSpace(SpaceReference spaceReference)
    {
        excludedSpaces.add(spaceReference);
    }

    public void addExcludedSpaces(Collection<SpaceReference> spaceReferences)
    {
        excludedSpaces.addAll(spaceReferences);
    }
    
    public void addIncludedSpace(SpaceReference spaceReference)
    {
        includedSpaces.add(spaceReference);
    }

    public void addIncludedSpaces(Collection<SpaceReference> spaceReferences)
    {
        includedSpaces.addAll(spaceReferences);
    }

    public void addExcludedObjectClass(DocumentReference classReference)
    {
        excludedObjectClasses.add(classReference);
    }

    public void addExcludedObjectClasses(Collection<DocumentReference> classReferences)
    {
        excludedObjectClasses.addAll(classReferences);
    }

    public void addExcludedPage(DocumentReference pageReference)
    {
        excludedPages.add(pageReference);
    }

    public void addExcludedPages(Collection<DocumentReference> pageReferences)
    {
        excludedPages.addAll(pageReferences);
    }

    public WikiReference getWikiReference()
    {
        return wikiReference;
    }

    public void setWikiReference(WikiReference wikiReference)
    {
        this.wikiReference = wikiReference;
    }

    public boolean isAddAutoRedirect()
    {
        return addAutoRedirect;
    }

    public void setAddAutoRedirect(boolean addAutoRedirect)
    {
        this.addAutoRedirect = addAutoRedirect;
    }

    public void addDisabledAction(String actionName)
    {
        disabledActions.add(actionName);
    }

    public boolean isActionEnabled(String actionName)
    {
        return !disabledActions.contains(actionName);
    }
}
