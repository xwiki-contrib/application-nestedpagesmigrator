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

import java.util.ArrayList;
import java.util.Collection;

import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.model.reference.DocumentReference;

/**
 * Represent a wiki page.
 *
 * @version $Id: $
 */
public class Page
{
    private DocumentReference documentReference;
    
    private DocumentReference parent;
    
    private DocumentReference from;
    
    private boolean isFailedToLoad;

    private boolean deletePrevious;

    private DocumentReference duplicateOf;

    private Collection<Preference> preferences = new ArrayList<>();

    private Collection<Right> rights = new ArrayList<>();

    public Page(DocumentReference documentReference, DocumentReference parent,
            DocumentReference from, boolean isFailedToLoad, DocumentReference duplicateOf, boolean deletePrevious)
    {
        this.documentReference = documentReference;
        this.parent = parent;
        this.from = from;
        this.isFailedToLoad = isFailedToLoad;
        this.duplicateOf = duplicateOf;
        this.deletePrevious = deletePrevious;
    }

    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    public DocumentReference getParent()
    {
        return parent;
    }

    public DocumentReference getFrom()
    {
        return from;
    }

    public boolean isFailedToLoad()
    {
        return isFailedToLoad;
    }

    public boolean shouldDeletePrevious()
    {
        return deletePrevious;
    }

    public DocumentReference getDuplicateOf()
    {
        return duplicateOf;
    }

    public Collection<Preference> getPreferences()
    {
        return preferences;
    }

    public void addPreference(Preference preference)
    {
        preferences.add(preference);
    }

    public Collection<Right> getRights()
    {
        return rights;
    }

    public void addRight(Right right)
    {
        rights.add(right);
    }
}
