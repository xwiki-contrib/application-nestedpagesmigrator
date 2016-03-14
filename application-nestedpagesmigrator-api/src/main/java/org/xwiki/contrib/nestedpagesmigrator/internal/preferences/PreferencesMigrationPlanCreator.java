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
package org.xwiki.contrib.nestedpagesmigrator.internal.preferences;

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
 * Add the actions concerning the preferences into a computed plan. Not thread safe.
 *
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

    /**
     * Handle the conversion of the preferences.
     *
     * @param plan the computed plan
     * @param configuration the configuration
     *
     * @throws MigrationException if error happens
     */
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

    /**
     * Make sure that the preferences concerning a document will remain the same after the execution of a migration
     * action. Handle the children actions too.
     *
     * @param action the action to convert
     */
    private void convertPreferences(MigrationAction action)
    {
        progressManager.startStep(this);

        // For each property, the inherited value should be the same after the action than before.
        for (String property : properties) {
            Preference valueBefore = getPreferenceValue(action.getSourceDocument(), property);
            Object valueAfter = getPreferenceValueAfter(action, property);
            boolean hasProperty = hasProperty(action.getSourceDocument(), property);
            if (valueBefore.getValue() != null && (!valueBefore.getValue().equals(valueAfter) || hasProperty)) {
                // If the value is different or if the value was manually set on the source document (even if the
                // inherited preference is the same), we add the preference to this action.
                action.addPreference(valueBefore);
            }
        }

        // Also handle children actions
        for (MigrationAction child : action.getChildren()) {
            convertPreferences(child);
        }
    }

    /**
     * @param documentReference the "WebPreferences" document containing the preferences
     * @param property the preference property yo check
     * @return either or not the property was set on this document, whatever is the value
     */
    private boolean hasProperty(DocumentReference documentReference, String property)
    {
        if (!"WebHome".equals(documentReference.getName())) {
            return false;
        }
        DocumentReference webPreferences
                = new DocumentReference("WebPreferences", documentReference.getLastSpaceReference());
        return !isNull(documentAccessBridge.getProperty(webPreferences, classReference, property));
    }

    /**
     * Get the preference value of a property in the WebPreferences document related to a document.
     *
     * @param document the document from where to get the value
     * @param propertyName the name of the property to look at
     *
     * @return the property value
     */
    private Preference getPreferenceValue(DocumentReference document, String propertyName)
    {
        return getPreferenceValue(document.getLastSpaceReference(), propertyName);
    }

    /**
     * Get the preference value of a property in the WebPreferences document in a given space.
     *
     * @param space the space to look at
     * @param propertyName the name of the property to look at
     *
     * @return the property value
     */
    private Preference getPreferenceValue(SpaceReference space, String propertyName)
    {
        DocumentReference webPreferences = new DocumentReference("WebPreferences", space);
        Object value = documentAccessBridge.getProperty(webPreferences, classReference, propertyName);

        if (isNull(value)) {
            // Fallback to the parent space
            EntityReference spaceParent = webPreferences.getLastSpaceReference().getParent();
            if (spaceParent.getType() == EntityType.SPACE) {
                return getPreferenceValue(new SpaceReference(spaceParent), propertyName);
            } else if (spaceParent.getType() == EntityType.WIKI) {
                return new Preference(propertyName,
                        documentAccessBridge.getProperty(classReference, classReference, propertyName), classReference);
            }
        }

        return new Preference(propertyName, value, webPreferences);
    }

    /**
     * Get the preference value of a property after a migration has been done.
     *
     * @param action the action that will be executed
     * @param propertyName the name of the property to look at
     *
     * @return the property value
     */
    private Object getPreferenceValueAfter(MigrationAction action, String propertyName)
    {
        // First: look if the action have a preference set
        if (action != null) {
            for (Preference preference : action.getPreferences()) {
                // If the preference's name of the action match the property name
                if (StringUtils.equals(preference.getName(), propertyName)) {
                    // It's this value which will be applied, so we return it
                    return preference.getValue();
                }
            }
        }

        // Get the value for the WebPreferences page of the target document
        DocumentReference webPreferences
                = new DocumentReference("WebPreferences", action.getTargetDocument().getLastSpaceReference());
        Object value = documentAccessBridge.getProperty(webPreferences, classReference, propertyName);

        // If the value is null, we must explore the parents, to get inherited preferences
        if (isNull(value)) {
            // Fallback to the parent
            EntityReference spaceParent = webPreferences.getLastSpaceReference().getParent();
            if (spaceParent.getType() == EntityType.SPACE) {
                // If the parent is a space, we get the document parent
                DocumentReference parent = new DocumentReference("WebHome", new SpaceReference(spaceParent));
                // We get action concerning this document
                //TODO: in practice, the parent action can be null
                MigrationAction parentAction = plan.getActionWithTarget(parent);
                // And we get the value from this action
                value = getPreferenceValueAfter(parentAction, propertyName);
            } else if (spaceParent.getType() == EntityType.WIKI) {
                // If the parent is the wiki, we get the wiki preferences
                value = documentAccessBridge.getProperty(classReference, classReference, propertyName);
                //TODO: look at the main wiki preferences too
            }
        }

        return value;
    }

    /**
     * @param value the value to check
     * @return if the value is considered as "null" (could be null, blank, or "--")
     */
    private boolean isNull(Object value)
    {
        if (value == null) {
            return true;
        }

        if (value instanceof String) {
            String stringValue = (String) value;
            return StringUtils.isBlank(stringValue) || "--".equals(stringValue);
        }

        return false;
    }
}
