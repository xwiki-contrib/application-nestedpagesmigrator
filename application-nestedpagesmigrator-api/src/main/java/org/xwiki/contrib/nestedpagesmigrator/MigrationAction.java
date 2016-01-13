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

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id: $
 */
public class MigrationAction
{
    private DocumentReference sourceDocument;
    
    private DocumentReference targetDocument;

    public MigrationAction(DocumentReference sourceDocument, DocumentReference targetDocument) throws MigrationException
    {
        setSourceDocument(sourceDocument);
        setTargetDocument(targetDocument);
    }

    public DocumentReference getSourceDocument()
    {
        return sourceDocument;
    }

    public void setSourceDocument(DocumentReference sourceDocument) throws MigrationException
    {
        if (sourceDocument == null) {
            throw new MigrationException("Source document cannot be null.");
        }
        this.sourceDocument = sourceDocument;
    }

    public DocumentReference getTargetDocument()
    {
        return targetDocument;
    }

    public void setTargetDocument(DocumentReference targetDocument) throws MigrationException
    {
        if (sourceDocument == null) {
            throw new MigrationException("Target document cannot be null.");
        }
        this.targetDocument = targetDocument;
    }
    
    public int getLevelOfNesting()
    {
        return targetDocument.getSpaceReferences().size();
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof MigrationAction) {
            MigrationAction otherAction = (MigrationAction) o;
            
            return sourceDocument.equals(otherAction.sourceDocument)
                    && targetDocument.equals(otherAction.targetDocument);
        }
        
        return false;
    }
}
