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

import java.lang.reflect.Type;
import java.util.Collection;

import org.xwiki.model.reference.DocumentReference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serialize a migration plan to a JSON tree.
 *  
 * @version $Id: $
 * @since 0.3
 */
public class MigrationPlanSerializer
{
    private static class DocumentReferenceSerializer implements JsonSerializer<DocumentReference>
    {
        @Override
        public JsonElement serialize(DocumentReference src, Type typeOfSrc, JsonSerializationContext context)
        {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class CollectionAdapter implements JsonSerializer<Collection<?>>
    {
        @Override
        public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context)
        {
            if (src == null || src.isEmpty()) {
                return null;
            }

            if (src.iterator().next() instanceof Preference) {
                // Special case: the preferences
                JsonObject object = new JsonObject();
                for (Object child : src) {
                    Preference preference = (Preference) child;
                    object.add(preference.getName(), context.serialize(preference.getValue()));
                }
                return object;
            } else {
                // Otherwise, apply the standard strategy
                JsonArray array = new JsonArray();
                for (Object child : src) {
                    JsonElement element = context.serialize(child);
                    array.add(element);
                }
                return array;
            }
        }
    }

    /**
     * Serialize the migration plan to a JSON tree (as string).
     * @param planTree plan to serialize
     * @return the JSON tree as string
     */
    public static String serialize(MigrationPlanTree planTree)
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder = gsonBuilder.registerTypeAdapter(DocumentReference.class, new DocumentReferenceSerializer());
        gsonBuilder = gsonBuilder.registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter());
        gsonBuilder = gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        return gson.toJson(planTree.getTopLevelAction().getChildren());
    }

}
