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
package org.xwiki.contrib.nestedpagesmigrator.internal.breakage;

import org.xwiki.model.reference.DocumentReference;

/**
 * See {@link HierarchyBreakageDetector}.
 *
 * @version $Id: $
 * @since 0.7
 */
public class Breakage
{
    private DocumentReference documentReference;

    private DocumentReference locationParent;

    private DocumentReference actualParent;

    /**
     * Construct a breakage object.
     * @param documentReference the reference of the document
     * @param locationParent the reference of the parent according to its location
     * @param actualParent the reference of the parent according to the "parent" field of the document
     */
    public Breakage(DocumentReference documentReference, DocumentReference locationParent,
            DocumentReference actualParent)
    {
        this.documentReference = documentReference;
        this.locationParent = locationParent;
        this.actualParent = actualParent;
    }

    /**
     * @return the reference of the broken document
     */
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    /**
     * @return the reference of the parent according to its location
     */
    public DocumentReference getLocationParent()
    {
        return locationParent;
    }

    /**
     * @return the reference of the parent according to the "parent" field of the document
     */
    public DocumentReference getActualParent()
    {
        return actualParent;
    }
}
