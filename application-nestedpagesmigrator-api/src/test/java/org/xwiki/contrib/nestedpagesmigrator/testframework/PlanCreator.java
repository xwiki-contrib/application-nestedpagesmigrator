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

import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Create a plan according to an {@link Example}.
 *
 * @version $Id: $
 */
public class PlanCreator
{
    /**
     * @param example the example to build
     * @return the plan according to the example
     *
     * @throws MigrationException if error happens
     */
    public static MigrationPlanTree createPlan(Example example) throws MigrationException
    {
        // Create a plan tree corresponding to the XML.
        // Note: the order of the page in the XML is important (parent must be declared before children) otherwise this
        // code will fail.
        MigrationPlanTree plan = new MigrationPlanTree();
        for (Page page : example.getAllPagesAfter()) {
            DocumentReference parent = getHierarchyParent(page.getDocumentReference());
            MigrationAction parentAction;
            if (parent == null) {
                parentAction = plan.getTopLevelAction();
            } else {
                parentAction = plan.getActionWithTarget(parent);
            }
            MigrationAction.createInstance(page.getFrom(), page.getDocumentReference(), parentAction, plan);
        }

        return plan;
    }

    private static DocumentReference getHierarchyParent(DocumentReference documentReference)
    {
        EntityReference spaceParent = documentReference.getLastSpaceReference().getParent();
        if (spaceParent.getType() == EntityType.SPACE) {
            return new DocumentReference("WebHome", new SpaceReference(spaceParent));
        } else {
            return null;
        }
    }
}
