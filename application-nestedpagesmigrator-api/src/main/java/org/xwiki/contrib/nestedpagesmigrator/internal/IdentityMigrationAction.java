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
package org.xwiki.contrib.nestedpagesmigrator.internal;

import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.model.reference.DocumentReference;

/**
 * The "identity" transformation transposed to migration actions: an action that does nothing. Useful when you need to
 * record not to do anything concerning a document.
 *
 * @version $Id: $
 */
public class IdentityMigrationAction extends MigrationAction
{
    /**
     * Helper to create an instance and record it in its parent and its plan.
     *  
     * @param documentReference the concerned document
     * @param parentAction the parent action
     * @param plan the plan
     *  
     * @return the created instance
     */
    public static IdentityMigrationAction createInstance(DocumentReference documentReference, MigrationAction parentAction,
            MigrationPlanTree plan)
    {
        IdentityMigrationAction action = new IdentityMigrationAction(documentReference);
        parentAction.addChild(action);
        plan.addAction(action);
        return action;
    }

    /**
     * Create a new IdentityMigrationAction.
     * @param documentReference the concerned document
     */
    public IdentityMigrationAction(DocumentReference documentReference)
    {
        // Doing nothing means having the same location as target.
        super(documentReference, documentReference);
    }
}
