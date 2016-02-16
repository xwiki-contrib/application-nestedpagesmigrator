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
package org.xwiki.contrib.nestedpagesmigrator.internal.rights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * @version $Id: $
 * @since 0.3
 */
@Component(roles = RightsMigrationPlanCreator.class)
@Singleton
public class RightsMigrationPlanCreator
{
    @Inject
    private JobProgressManager progressManager;

    @Inject
    private DocumentRightsBridge documentRightsBridge;

    public void convertRights(MigrationPlanTree plan, MigrationConfiguration configuration) throws MigrationException
    {
        progressManager.pushLevelProgress(plan.getActions().size(), this);
        for (MigrationAction action : plan.getTopLevelAction().getChildren()) {
            convertRights(action, plan);
        }
        progressManager.popLevelProgress(this);
    }

    private void convertRights(MigrationAction action, MigrationPlanTree plan) throws MigrationException
    {
        progressManager.startStep(this);

        Collection<Right> oldRights = getRightsFromHierarchy(action.getSourceDocument(), null);
        Collection<Right> newRights = getRightsFromHierarchy(action.getTargetDocument(), plan);

        // Preserve all old rights
        for (Right oldRight : oldRights) {
            boolean found = false;
            for (Right newRight : newRights) {
                if (oldRight.equals(newRight)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // We need to add the right to the action
                action.addRight(oldRight);
            }
        }

        // Now make sure we don't have inherited right that was not there before
        for (Right newRight : newRights) {
            boolean found = false;
            for (Right oldRight : oldRights) {
                if (oldRight.equals(newRight)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // A new right have been inherited. We need to dismiss it!
                action.addRight(newRight.getInverseRight());
            }
        }

        // Convert children too
        for (MigrationAction child : action.getChildren()) {
            convertRights(child, plan);
        }
    }

    private Collection<Right> getRightsFromHierarchy(DocumentReference documentReference, MigrationPlanTree plan)
        throws MigrationException
    {
        Collection<Right> rights = new ArrayList<>();
        getRightsFromHierarchy(documentReference.getLastSpaceReference(), rights, plan);
        return rights;
    }

    private void getRightsFromHierarchy(SpaceReference spaceReference, Collection<Right> rights, MigrationPlanTree plan)
            throws MigrationException
    {
        if (plan != null) {
            MigrationAction action = plan.getActionWithTarget(new DocumentReference("WebHome", spaceReference));
            if (action != null) {
                // Copy the list because addRightsIfNotSameConcern() modify it and we don't want to modify the action.
                List<Right> actionRights = new LinkedList<>(action.getRights());
                addRightsIfNotSameConcern(actionRights, rights);
            }
        }

        getRightsFromDocument(new DocumentReference("WebPreferences", spaceReference), rights);

        // Now parse the parent
        EntityReference spaceParent = spaceReference.getParent();
        if (spaceParent.getType() == EntityType.SPACE) {
            getRightsFromHierarchy(new SpaceReference(spaceParent), rights, plan);
        } else if (spaceParent.getType() == EntityType.WIKI) {
            DocumentReference wikiPreferences = new DocumentReference(spaceReference.getWikiReference().getName(),
                    "XWiki", "XWikiPreferences");
            getRightsFromDocument(wikiPreferences, rights);
        }
    }

    private void getRightsFromDocument(DocumentReference document, Collection<Right> rights)
            throws MigrationException
    {
        Collection<Right> localRights = documentRightsBridge.getRights(document);
        addRightsIfNotSameConcern(localRights, rights);
    }

    private void addRightsIfNotSameConcern(Collection<Right> rightsToAdd, Collection<Right> currentRights)
    {
        Iterator<Right> it = rightsToAdd.iterator();
        while (it.hasNext()) {
            Right localRight = it.next();
            for (Right currentRight : currentRights) {
                // Same right already configured by descendant, so we ignore it
                if (localRight.hasSameConcern(currentRight)) {
                    it.remove();
                    break;
                }
            }
        }
        currentRights.addAll(rightsToAdd);
    }
}
