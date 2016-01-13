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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * @version $Id: $
 */
public class MigrationConfiguration
{
    private boolean excludeHiddenPages;
    
    private boolean excludeClassPages;
    
    private boolean dontMoveChildren;
    
    private boolean addAutoRedirect;
    
    private List<SpaceReference> excludedSpaces = new ArrayList<>();
    
    private List<SpaceReference> includedSpaces = new ArrayList<>();

    private List<DocumentReference> excludedObjectClasses = new ArrayList<>();
    
    private WikiReference wikiReference;
    
    public MigrationConfiguration(WikiReference wikiReference)
    {
        excludeHiddenPages = true;
        excludeClassPages = true;
        dontMoveChildren = false;
        addAutoRedirect = true;
        this.wikiReference = wikiReference;
    }
    
    public boolean hasIncludedSpaces()
    {
        return !includedSpaces.isEmpty();
    }

    public boolean hasExcludedSpaces()
    {
        return !excludedSpaces.isEmpty();
    }

    public boolean hasExcludedObjectClasses()
    {
        return !excludedObjectClasses.isEmpty();
    }

    public boolean isExcludeHiddenPages()
    {
        return excludeHiddenPages;
    }

    public void setExcludeHiddenPages(boolean excludeHiddenPages)
    {
        this.excludeHiddenPages = excludeHiddenPages;
    }

    public boolean isExcludeClassPages()
    {
        return excludeClassPages;
    }

    public void setExcludeClassPages(boolean excludeClassPages)
    {
        this.excludeClassPages = excludeClassPages;
    }

    public boolean isDontMoveChildren()
    {
        return dontMoveChildren;
    }

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

    public void addExcludedSpace(SpaceReference spaceReference)
    {
        excludedSpaces.add(spaceReference);
    }
    
    public void addIncludedSpace(SpaceReference spaceReference)
    {
        includedSpaces.add(spaceReference);
    }

    public void addExcludedObjectClass(DocumentReference classReference)
    {
        excludedObjectClasses.add(classReference);
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
}
