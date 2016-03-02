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
package org.xwiki.contrib.nestedpagesmigrator.internal.pages;

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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
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
 * Get the list of pages that must be converted.
 *
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

    /**
     * @param configuration the configuration of the migration
     *
     * @return the list of pages to convert
     *
     * @throws MigrationException if error happens
     */
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
            // TODO: to scale efficiently, add a limit() and an offset()
            
            // Then load each document to see if they match the criteria
            progressManager.startStep(this);
            
            // Send how many document are going to be read
            progressManager.pushLevelProgress(docNames.size(), this);
            for (String docName : docNames) {
                progressManager.startStep(this);
                
                DocumentReference documentReference =
                        documentReferenceResolver.resolve(docName, configuration.getWikiReference());

                try {
                    XWikiDocument doc = xwiki.getDocument(documentReference, context);

                    // If the document is not terminal and already under its parent, exclude it
                    if (isNotTerminal(documentReference) && isAlreadyUnderParent(documentReference, doc)) {
                        continue;
                    }

                    // If the document holds a class and the configuration say do not convert such a page, exclude it
                    if (configuration.isExcludeClassPages() && !doc.getXClass().getPropertyList().isEmpty()) {
                        continue;
                    }

                    // If the document holds a forbidden object, exclude it
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
            // A 'WebHome' document might have a wrong parent, so we only exclude WebPreferences documents
            xwql.append("where doc.name <> 'WebPreferences'");
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

    /**
     * This method is needed because we cannot write it directly in the SQL query. It's a shame...
     *
     * @param documentReference the document reference
     * @param document the corresponding XWiki document
     * @return if the document is under its parent (so we don't need to return it)
     */
    private boolean isAlreadyUnderParent(DocumentReference documentReference, XWikiDocument document)
    {
        EntityReference spaceParent = documentReference.getLastSpaceReference().getParent();
        if (spaceParent.getType() == EntityType.SPACE) {
            // Return if the WebHome page of the parent space of the space is the parent set in the document
            // [A.B.WebHome must have A.WebHome as parent].
            DocumentReference expectedParent = new DocumentReference("WebHome", new SpaceReference(spaceParent));
            return expectedParent.equals(document.getParentReference());
        } else {
            // The document is a top-level document [A.WebHome], so its parent must be itself or null
            DocumentReference parent = document.getParentReference();
            return parent == null || parent.equals(documentReference);
        }
    }

    /**
     * @param doc the document to verify
     * @param excludedObjectClasses the list of forbidden classes
     * @return either or not the document contains an instance of a forbidden class
     */
    private boolean hasForbiddenObject(XWikiDocument doc, List<DocumentReference> excludedObjectClasses)
    {
        for (DocumentReference classReference : excludedObjectClasses) {
            if (doc.getXObjectSize(classReference) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param documentReference a reference to a document
     * @return if the document is not terminal
     */
    private boolean isNotTerminal(DocumentReference documentReference)
    {
        return documentReference.getName().equals("WebHome");
    }
}
