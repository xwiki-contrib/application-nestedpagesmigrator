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
import java.util.List;

import org.xwiki.model.reference.DocumentReference;

/**
 * An atomic operation of a {@link MigrationPlanTree}.
 *  
 * @version $Id: $
 */
public class MigrationAction implements Serializable, Comparable
{
    private DocumentReference sourceDocument;
    
    private DocumentReference targetDocument;
    
    private List<MigrationAction> children = new ArrayList<>();

    /**
     * Helper to create an instance and record it in its plan.
     *
     * @param sourceDocument the source document
     * @param targetDocument the target location
     * @param plan the plan
     *
     * @return the created instance
     */
    public static MigrationAction createInstance(DocumentReference sourceDocument, DocumentReference targetDocument,
            MigrationPlanTree plan) throws MigrationException
    {
        MigrationAction action = new MigrationAction(sourceDocument, targetDocument);
        plan.addAction(action);
        return action;
    }

    /**
     * Helper to create an instance and record it in its parent and its plan.
     *
     * @param sourceDocument the source document
     * @param targetDocument the target location
     * @param parentAction the parent action
     * @param plan the plan
     *
     * @return the created instance
     */
    public static MigrationAction createInstance(DocumentReference sourceDocument, DocumentReference targetDocument,
            MigrationAction parentAction, MigrationPlanTree plan) throws MigrationException
    {
        MigrationAction action = new MigrationAction(sourceDocument, targetDocument);
        parentAction.addChild(action);
        plan.addAction(action);
        return action;
    }

    /**
     * Construct a new MigrationAction.
     * @param sourceDocument the source document
     * @param targetDocument the target location
     */
    public MigrationAction(DocumentReference sourceDocument, DocumentReference targetDocument)
    {
        this.sourceDocument = sourceDocument;
        this.targetDocument = targetDocument;
    }

    public DocumentReference getSourceDocument()
    {
        return sourceDocument;
    }

    public DocumentReference getTargetDocument()
    {
        return targetDocument;
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
    
    public void addChild(MigrationAction action)
    {
        children.add(action);
    }
    
    public List<MigrationAction> getChildren()
    {
        return children;
    }

    @Override
    public int compareTo(Object o)
    {
        if (o instanceof MigrationAction) {
            MigrationAction otherAction = (MigrationAction) o;
            return this.getTargetDocument().getLastSpaceReference().getName().compareTo(
                    otherAction.getTargetDocument().getLastSpaceReference().getName());
        }
        
        return 0;
    }
}
