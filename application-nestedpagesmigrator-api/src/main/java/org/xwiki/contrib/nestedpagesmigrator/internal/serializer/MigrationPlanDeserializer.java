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
package org.xwiki.contrib.nestedpagesmigrator.internal.serializer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.Preference;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @version $Id: $
 */
@Component(roles = MigrationPlanDeserializer.class)
@Singleton
public class MigrationPlanDeserializer
{
    @Inject
    private DocumentReferenceResolver<String> resolver;

    public MigrationPlanTree deserialize(String json, WikiReference wikiReference) throws MigrationException
    {
        MigrationPlanTree plan = new MigrationPlanTree();

        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(json);

        if (!root.isJsonArray()) {
            throw new MigrationException("Unexpected JSON schema.");
        }

        for (JsonElement item : root.getAsJsonArray()) {
            JsonObject action = item.getAsJsonObject();
            readMigrationAction(action, plan, plan.getTopLevelAction(), wikiReference);
        }

        return plan;
    }

    private void readMigrationAction(JsonObject jsonAction, MigrationPlanTree plan, MigrationAction parent,
            WikiReference wikiReference)
            throws MigrationException
    {
        DocumentReference sourceDocument =
                resolver.resolve(jsonAction.getAsJsonPrimitive("sourceDocument").getAsString(), wikiReference);
        DocumentReference targetDocument =
                resolver.resolve(jsonAction.getAsJsonPrimitive("targetDocument").getAsString(), wikiReference);

        MigrationAction action = MigrationAction.createInstance(sourceDocument, targetDocument, parent, plan);
        if (jsonAction.has("enabled") && jsonAction.getAsJsonPrimitive("enabled").getAsString().equals("false")) {
            action.setEnabled(false);
        }

        if (jsonAction.has("children")) {
            for (JsonElement item : jsonAction.getAsJsonArray("children")) {
                readMigrationAction(item.getAsJsonObject(), plan, action, wikiReference);
            }
        }

        if (jsonAction.has("preferences")) {
            for (JsonElement item : jsonAction.getAsJsonArray("preferences")) {
                JsonObject jsonPreference = item.getAsJsonObject();
                action.addPreference(new Preference(
                        jsonPreference.getAsJsonPrimitive("name").getAsString(),
                        jsonPreference.getAsJsonPrimitive("value").getAsString(),
                        resolver.resolve(jsonPreference.getAsJsonPrimitive("origin").getAsString(), wikiReference)
                ));
            }
        }

        if (jsonAction.has("rights")) {
            for (JsonElement item : jsonAction.getAsJsonArray("rights")) {
                JsonObject jsonRight = item.getAsJsonObject();

                DocumentReference user = null;
                DocumentReference group = null;

                if (jsonRight.has("user")) {
                    user = resolver.resolve(jsonRight.getAsJsonPrimitive("user").getAsString(), wikiReference);
                } else if (jsonRight.has("group")) {
                    group = resolver.resolve(jsonRight.getAsJsonPrimitive("group").getAsString(), wikiReference);
                }

                action.addRight(new Right(
                        user,
                        group,
                        jsonRight.getAsJsonPrimitive("level").getAsString(),
                        "true".equalsIgnoreCase(jsonRight.getAsJsonPrimitive("allow").getAsString()),
                        resolver.resolve(jsonRight.getAsJsonPrimitive("origin").getAsString(), wikiReference)
                ));
            }
        }
    }
}
