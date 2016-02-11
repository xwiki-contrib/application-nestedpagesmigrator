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

import java.util.Collection;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.text.StringUtils;

/**
 * Not thread safe.
 * @version $Id: $
 */
@Component(roles = PreferencesMigrationPlanCreator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PreferencesMigrationPlanCreator
{
    @Inject
    private PreferencesPropertiesGetter preferencesPropertiesGetter;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private JobProgressManager progressManager;

    private Collection<String> properties;

    private DocumentReference classReference;

    private MigrationPlanTree plan;

    public void convertPreferences(MigrationPlanTree plan, MigrationConfiguration configuration)
            throws MigrationException
    {
        this.plan = plan;
        classReference = new DocumentReference(configuration.getWikiReference().getName(), "XWiki", "XWikiPreferences");
        properties = preferencesPropertiesGetter.getPreferencesProperties();

        progressManager.pushLevelProgress(plan.getActions().size(), this);
        for (MigrationAction action : plan.getTopLevelAction().getChildren()) {
            convertPreferences(action);
        }
        progressManager.popLevelProgress(this);
    }

    private void convertPreferences(MigrationAction action)
    {
        progressManager.startStep(this);

        for (String property : properties) {
            Object valueBefore = getPreferenceValue(action.getSourceDocument(), property);
            Object valueAfter = getPreferenceValueAfter(action, property);
            boolean hasProperty = hasProperty(action.getSourceDocument(), property);
            if (valueBefore != null && (!valueBefore.equals(valueAfter) || hasProperty)) {
                // Do something here
                action.addPreference(new Preference(property, valueBefore));
            }
        }

        for (MigrationAction child : action.getChildren()) {
            convertPreferences(child);
        }
    }

    private boolean hasProperty(DocumentReference documentReference, String property)
    {
        if (!"WebHome".equals(documentReference.getName())) {
            return false;
        }
        DocumentReference webPreferences
                = new DocumentReference("WebPreferences", documentReference.getLastSpaceReference());
        return (documentAccessBridge.getProperty(webPreferences, classReference, property) != null);
    }

    private Object getPreferenceValue(DocumentReference document, String propertyName)
    {
        return getPreferenceValue(document.getLastSpaceReference(), propertyName);
    }

    private Object getPreferenceValue(SpaceReference space, String propertyName)
    {
        DocumentReference webPreferences = new DocumentReference("WebPreferences", space);
        Object value = documentAccessBridge.getProperty(webPreferences, classReference, propertyName);

        if (value == null) {
            // Fallback to the parent space
            EntityReference spaceParent = webPreferences.getLastSpaceReference().getParent();
            if (spaceParent.getType() == EntityType.SPACE) {
                value = getPreferenceValue(new SpaceReference(spaceParent), propertyName);
            } else if (spaceParent.getType() == EntityType.WIKI) {
                value = documentAccessBridge.getProperty(classReference, classReference, propertyName);
            }
        }

        return value;
    }

    private Object getPreferenceValueAfter(MigrationAction action, String propertyName)
    {
        for (Preference preference : action.getPreferences()) {
            if (StringUtils.equals(preference.getName(), propertyName)) {
                return preference.getValue();
            }
        }

        DocumentReference webPreferences
                = new DocumentReference("WebPreferences", action.getTargetDocument().getLastSpaceReference());
        Object value = documentAccessBridge.getProperty(webPreferences, classReference, propertyName);

        if (value == null) {
            // Fallback to the parent
            EntityReference spaceParent = webPreferences.getLastSpaceReference().getParent();
            if (spaceParent.getType() == EntityType.SPACE) {
                // Parent document
                DocumentReference parent = new DocumentReference("WebHome", new SpaceReference(spaceParent));
                // Get action concerning this document
                MigrationAction parentAction = plan.getActionWithTarget(parent);
                // Get the value from there
                value = getPreferenceValueAfter(parentAction, propertyName);
            } else if (spaceParent.getType() == EntityType.WIKI) {
                value = documentAccessBridge.getProperty(classReference, classReference, propertyName);
            }
        }

        return value;
    }
}
