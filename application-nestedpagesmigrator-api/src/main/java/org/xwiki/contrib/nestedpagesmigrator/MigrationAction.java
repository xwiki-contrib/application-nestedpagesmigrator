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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.text.StringUtils;

/**
 * An atomic operation of a {@link MigrationPlanTree}.
 * 
 * @version $Id: $
 */
public class MigrationAction implements Serializable, Comparable
{
    private DocumentReference sourceDocument;

    private DocumentReference targetDocument;

    private Collection<Preference> preferences;

    private Collection<Right> rights;

    private List<MigrationAction> children;

    /**
     * @see #shouldDeletePrevious()
     */
    private boolean deletePrevious = false;

    /**
     * @see #isEnabled()
     */
    private boolean enabled = true;

    /**
     * Helper to create an instance and record it in its plan.
     *
     * @param sourceDocument the source document
     * @param targetDocument the target location
     * @param plan the plan
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
     * Helper to create an instance and record it in its plan.
     *
     * @param sourceDocument the source document
     * @param targetReference the target location
     * @param plan the plan
     * @return the created instance
     */
    public static MigrationAction createInstance(DocumentReference sourceDocument, TargetReference targetReference,
        MigrationPlanTree plan) throws MigrationException
    {
        MigrationAction action = createInstance(sourceDocument, targetReference.getTargetDocument(), plan);
        action.setDeletePrevious(targetReference.getState() == TargetState.DUPLICATE);
        return action;
    }

    /**
     * Helper to create an instance and record it in its parent and its plan.
     *
     * @param sourceDocument the source document
     * @param targetDocument the target location
     * @param parentAction the parent action
     * @param plan the plan
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
     * Helper to create an instance and record it in its parent and its plan.
     *
     * @param sourceDocument the source document
     * @param targetReference the target location
     * @param parentAction the parent action
     * @param plan the plan
     * @return the created instance
     */
    public static MigrationAction createInstance(DocumentReference sourceDocument, TargetReference targetReference,
        MigrationAction parentAction, MigrationPlanTree plan) throws MigrationException
    {
        MigrationAction action = new MigrationAction(sourceDocument, targetReference.getTargetDocument());
        action.setDeletePrevious(targetReference.getState() == TargetState.DUPLICATE);
        parentAction.addChild(action);
        plan.addAction(action);
        return action;
    }

    /**
     * Helper to create an instance and record it in its parent and its plan.
     *
     * @param sourceDocument the source document
     * @param targetReference the target location
     * @param parentAction the parent action
     * @param plan the plan
     * @param enabled if the user has enabled the action
     * @return the created instance
     */
    public static MigrationAction createInstance(DocumentReference sourceDocument, TargetReference targetReference,
        MigrationAction parentAction, MigrationPlanTree plan, boolean enabled) throws MigrationException
    {
        MigrationAction action = createInstance(sourceDocument, targetReference, parentAction, plan);
        action.setEnabled(enabled);
        return action;
    }

    /**
     * Construct a new MigrationAction.
     * 
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

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MigrationAction) {
            MigrationAction otherAction = (MigrationAction) o;

            return sourceDocument.equals(otherAction.sourceDocument)
                && targetDocument.equals(otherAction.targetDocument);
        }

        return false;
    }

    /**
     * @return if the target must be removed before the action is performed
     */
    public boolean shouldDeletePrevious()
    {
        return deletePrevious;
    }

    public void setDeletePrevious(boolean deletePrevious)
    {
        this.deletePrevious = deletePrevious;
    }

    /**
     * @return if the action has not be disabled by the user
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void addChild(MigrationAction action)
    {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(action);
    }

    public List<MigrationAction> getChildren()
    {
        return children != null ? children : Collections.<MigrationAction>emptyList();
    }

    @Override
    public int compareTo(Object o)
    {
        if (o instanceof MigrationAction) {
            MigrationAction otherAction = (MigrationAction) o;
            return this.getTargetDocument().getLastSpaceReference().getName()
                .compareTo(otherAction.getTargetDocument().getLastSpaceReference().getName());
        }

        return 0;
    }

    @Override
    public String toString()
    {
        return String.format("[%s] -> [%s]", sourceDocument, targetDocument);
    }

    /**
     * @return the collection of preferences to store in the WebPreferences page.
     */
    public Collection<Preference> getPreferences()
    {
        return preferences != null ? preferences : Collections.<Preference>emptyList();
    }

    /**
     * Add a preference to store in the WebPreferences after the page conversion.
     * 
     * @param newPreference preference to add
     */
    public void addPreference(Preference newPreference)
    {
        if (preferences == null) {
            preferences = new ArrayList<>();
        }
        Iterator<Preference> it = preferences.iterator();
        while (it.hasNext()) {
            Preference oldPreference = it.next();
            if (StringUtils.equals(oldPreference.getName(), newPreference.getName())) {
                // remove the old preference
                it.remove();
            }
        }
        preferences.add(newPreference);
    }

    public Collection<Right> getRights()
    {
        return rights != null ? rights : Collections.<Right>emptyList();
    }

    public void addRight(Right newRight)
    {
        if (rights == null) {
            rights = new ArrayList<>();
        }
        Iterator<Right> it = rights.iterator();
        while (it.hasNext()) {
            Right oldRight = it.next();
            if (oldRight.hasSameConcern(newRight)) {
                // remove the right having the same concern
                it.remove();
            }
        }
        rights.add(newRight);
    }

    /**
     * @return the reference of the WebPreferences document that should store preferences concerning the target
     *         document.
     */
    public DocumentReference getWebPreferencesReference()
    {
        return new DocumentReference("WebPreferences", targetDocument.getLastSpaceReference());
    }

    /**
     * @return if the target is the same than the source
     */
    public boolean isIdentity()
    {
        return sourceDocument.equals(targetDocument);
    }

    /**
     * @return if the action has preferences
     */
    public boolean hasPreferences()
    {
        return preferences != null && !preferences.isEmpty();
    }

    /**
     * @return if some rights are configured
     */
    public boolean hasRights()
    {
        return rights != null && !rights.isEmpty();
    }
}
