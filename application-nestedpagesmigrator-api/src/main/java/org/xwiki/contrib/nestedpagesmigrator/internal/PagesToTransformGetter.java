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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id: $
 */
@Component(roles = PagesToTransformGetter.class)
@Singleton
public class PagesToTransformGetter
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> referenceSerializer;

    public List<DocumentReference> getPagesToConvert(MigrationConfiguration configuration) throws MigrationException
    {  
        try {
            Query query = getQuery(configuration);

            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();

            List<DocumentReference> results = new ArrayList<>();
            List<DocumentReference> excludedObjectClasses = configuration.getExcludedObjectClasses();

            progressManager.pushLevelProgress(2, this);
            
            // First: perform the query
            progressManager.startStep(this);
            List<String> docNames = query.execute();
            
            // Then load each document to see if they match the criteria
            progressManager.startStep(this);
            
            // Send how many document are going to be read
            progressManager.pushLevelProgress(docNames.size());
            for (String docName : docNames) {
                progressManager.startStep(this);
                
                DocumentReference documentReference =
                        documentReferenceResolver.resolve(docName, configuration.getWikiReference());

                try {
                    XWikiDocument doc = xwiki.getDocument(documentReference, context);
                    if (configuration.isExcludeClassPages() && !doc.getXClass().getPropertyList().isEmpty()) {
                        continue;
                    }

                    if (hasForbiddenObject(doc, excludedObjectClasses)) {
                        continue;
                    }

                } catch (XWikiException e) {
                    // TODO: maybe continue without this document but add something in the logs?
                    throw new MigrationException(
                            String.format("Failed to get the document [%s]", documentReference), e);
                }

                results.add(documentReference);
            }
            // All documents have been loaded
            progressManager.popLevelProgress(this);
            
            // This method is ended
            progressManager.popLevelProgress(this);

            return results;
        } catch (QueryException | ComponentLookupException e) {
            throw new MigrationException("Failed to get the list of terminal pages.", e);
        }
    }

    private Query getQuery(MigrationConfiguration configuration) throws ComponentLookupException, QueryException
    {
        StringBuilder xwql = new StringBuilder();

        if (configuration.isDontMoveChildren()) {
            // Only terminal documents are concerned, and WebPreferences should not be touched
            xwql.append("where doc.name not in ('WebHome', 'WebPreferences')");
        } else {
            // We need a different query here because in that case, we can move a non terminal page if we detect that it
            // is not under its parent.
            xwql.append(
                "where (doc.parent <> concat(doc.space, '.WebHome') or doc.name <> 'WebHome') " +
                        "and doc.name <> 'WebPreferences'");
        }

        // Exclude the preferences of the wiki!
        xwql.append(" and doc.fullName <> 'XWiki.XWikiPreferences'");

        if (configuration.hasIncludedSpaces()) {
            xwql.append(" and doc.space in (:includedSpaceList)");
        }
        if (configuration.hasExcludedSpaces()) {
            xwql.append(" and doc.space not in (:excludedSpaceList)");
        }
        if (configuration.hasExcludedPages()) {
            xwql.append(" and doc.fullName not in (:excludedDocList)");   
        }
        
        // It's important since the results could change because of the order (@see MigrationPlanCreator#createAction).
        xwql.append(" order by doc.fullName");

        Query query = queryManager.createQuery(xwql.toString(), Query.XWQL);
        query.setWiki(configuration.getWikiReference().getName());
        query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"));

        if (configuration.isExcludeHiddenPages()) {
            query.addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "hidden"));
        }

        if (configuration.hasIncludedSpaces()) {
            List<SpaceReference> includedSpaces = configuration.getIncludedSpaces();
            List<String> serializedIncludedSpaces = new ArrayList<>(includedSpaces.size());
            for (SpaceReference spaceReference : includedSpaces) {
                serializedIncludedSpaces.add(referenceSerializer.serialize(spaceReference));
            }
            query.bindValue("includedSpaceList", serializedIncludedSpaces);
        }

        if (configuration.hasExcludedSpaces()) {
            List<SpaceReference> excludedSpaces = configuration.getExcludedSpaces();
            List<String> serializedExcludedSpaces = new ArrayList<>(excludedSpaces.size());
            for (SpaceReference spaceReference : excludedSpaces) {
                serializedExcludedSpaces.add(referenceSerializer.serialize(spaceReference));
            }
            query.bindValue("excludedSpaceList", serializedExcludedSpaces);
        }

        if (configuration.hasExcludedPages()) {
            List<DocumentReference> excludedPages = configuration.getExcludedPages();
            List<String> serializedExcludedPages = new ArrayList<>(excludedPages.size());
            for (DocumentReference documentReference : excludedPages) {
                serializedExcludedPages.add(referenceSerializer.serialize(documentReference));
            }
            query.bindValue("excludedDocList", serializedExcludedPages);
        }

        return query;
    }

    private boolean hasForbiddenObject(XWikiDocument doc, List<DocumentReference> excludedObjectClasses)
    {
        for (DocumentReference classReference : excludedObjectClasses) {
            if (doc.getXObjectSize(classReference) > 0) {
                return true;
            }
        }
        return false;
    }
}
