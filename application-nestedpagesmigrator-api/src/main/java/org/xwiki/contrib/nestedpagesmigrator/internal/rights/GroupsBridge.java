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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * @version $Id: $
 */
@Component(roles = GroupsBridge.class)
public class GroupsBridge
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    private Map<DocumentReference, Collection<DocumentReference>> map = new HashMap<>();

    public boolean isMemberOf(DocumentReference entity, DocumentReference group, WikiReference wiki)
    {
        // First, what are the groups of the entity
        Collection<DocumentReference> groups = map.get(group);
        if (groups == null) {
            groups = getGroupsOf(entity, wiki);
            map.put(entity, groups);
        }
        return groups.contains(group);
    }

    private Collection<DocumentReference> getGroupsOf(DocumentReference entity, WikiReference wiki)
    {
        HashSet<DocumentReference> groups = new HashSet();

        getGroupsOf(entity, wiki, groups);

        return groups;
    }

    private void getGroupsOf(DocumentReference entity, WikiReference wiki, HashSet<DocumentReference> groups)
    {
        try {
            String xwql = "from doc.object(XWiki.XWikiGroups) obj where obj.member in (:globalEntity, :localEntity)";
            Query query = queryManager.createQuery(xwql, Query.XWQL);

            query.bindValue("globalEntity", defaultSerializer.serialize(entity));
            query.bindValue("localEntity", localSerializer.serialize(entity));

            query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"));

            query.setWiki(wiki.getName());

            List<String> results = query.execute();
            for (String result : results) {
                DocumentReference groupReference = documentReferenceResolver.resolve(result, wiki);
                if (groups.add(groupReference)) {
                    getGroupsOf(groupReference, wiki, groups);
                }
            }

            // Run the same query in the main wiki
            if (!wikiDescriptorManager.getMainWikiId().equals(wiki.getName())) {
                query.setWiki(wikiDescriptorManager.getMainWikiId());
                results = query.execute();
                WikiReference mainWiki = new WikiReference(wikiDescriptorManager.getMainWikiId());
                for (String result : results) {
                    DocumentReference groupReference = documentReferenceResolver.resolve(result, mainWiki);
                    if (groups.add(groupReference)) {
                        getGroupsOf(groupReference, wiki, groups);
                    }
                }
            }

        } catch (QueryException | ComponentLookupException  e) {
            e.printStackTrace();
        }
    }
}
