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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A thread-safe migration plan creator which computes ALL required actions to convert a wiki to Nested Pages. 
 *
 * @version $Id: $
 */
@Component(roles = MigrationPlanCreator.class)
@Singleton
public class MigrationPlanCreator
{
    private static final String SPACE_HOME_PAGE = "WebHome";
    
    @Inject
    private TerminalPagesGetter terminalPagesGetter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Compute all migration actions needed to convert a wiki to Nested Pages.
     *
     * @param configuration the configuration
     * @return the migration plan tree
     * @throws MigrationException if problems occur
     */
    public MigrationPlanTree computeMigrationPlan(MigrationConfiguration configuration) throws MigrationException
    {
        List<DocumentReference> terminalDocs = terminalPagesGetter.getTerminalPages(configuration);
        MigrationPlanTree plan = new MigrationPlanTree();

        XWikiContext context = contextProvider.get();

        if (configuration.isDontMoveChildren()) {
            for (DocumentReference terminalDoc : terminalDocs) {
                convertDocumentWithoutMove(terminalDoc, plan);
            }
        } else {
            for (DocumentReference terminalDoc : terminalDocs) {
                convertDocumentAndParents(terminalDoc, plan, terminalDocs, context);
            }
        }

        // A sorted migration plan tree is more user-friendly.
        plan.sort();
        return plan;
    }

    /**
     * Convert a terminal document to nested pages without trying to move it under its parents.
     *  
     * @param terminalDoc the document to convert
     * @param plan the migration plan
     *  
     * @return the action concerning this document
     */
    private MigrationAction convertDocumentWithoutMove(DocumentReference terminalDoc, MigrationPlanTree plan)
    {
        SpaceReference parentSpace = new SpaceReference(terminalDoc.getName(), terminalDoc.getLastSpaceReference());
        DocumentReference targetDoc = new DocumentReference(SPACE_HOME_PAGE, parentSpace);
        return MigrationAction.createInstance(terminalDoc, targetDoc, plan);
    }

    /**
     * Convert a document and all its parents to Nested Pages.
     *  
     * @param documentReference the document to convert
     * @param plan the migration plan to fill
     * @param concernedDocuments the list of concerned documents. Only document from this list will be changed.
     * @param context the XWiki context
     *  
     * @return the migration of the document
     */
    private MigrationAction convertDocumentAndParents(DocumentReference documentReference, MigrationPlanTree plan,
            List<DocumentReference> concernedDocuments,
            XWikiContext context) throws MigrationException
    {
        // A planned action concerning this document might have been created already. We avoid recomputing the 
        // conversion by returning the existing action, if there is any.
        MigrationAction existingAction = plan.getActionAbout(documentReference);
        if (existingAction != null) {
            return existingAction;
        }

        // Since the user might have configured some exclusions, we verify that this document is contained in the list
        // of documents to convert.
        if (!concernedDocuments.contains(documentReference)) {
            // Otherwise, we create an "identity" action: it does nothing but it will added to the plan so that action
            // won't be recomputed afterwards.
            // Note that this action is added as child of the top-level action, because we want to have it in the plan 
            // tree.
            return IdentityMigrationAction.createInstance(documentReference, plan.getTopLevelAction(), plan);
        }

        // Now we are sure that the document needs to be converted, so let's start the real job.
        // --

        // Get the parent reference
        DocumentReference parentReference;
        try {
            parentReference = getParent(documentReference, context);
        } catch (XWikiException e) {
            logger.error("Failed to open the document [{}].", documentReference, e);
            // Don't fail the migration just because of that, return an identity action instead.
            return new IdentityMigrationAction(documentReference);
        }

        // Get the action concerning the parent (or the top level action if the document is orphan). Because a plan 
        // concerning the parents must have been prepared before we compute the plan for the current document.
        MigrationAction parentAction = parentReference != null ?
                convertDocumentAndParents(parentReference, plan, concernedDocuments, context)
                : plan.getTopLevelAction();

        // Now, create the action for the current document
        return createAction(documentReference, parentAction, plan);
    }

    /**
     * Get the parent of a document.
     *  
     * @param documentReference the document
     * @param context the XWiki Context
     *  
     * @return the parent document, or null if the document is and remains orphan
     *  
     * @throws XWikiException if the document cannot be loaded
     */
    private DocumentReference getParent(DocumentReference documentReference, XWikiContext context) throws XWikiException
    {
        // To read the "parent" field of the document, we need to load the document
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);
        DocumentReference parentReference = document.getParentReference();

        // The parent field might not be filled. In that case, the document is actually an orphan.
        if (parentReference == null) {
            // What should we do ?
            // Strategy 1: don't do anything
            // Strategy 2: use the space ancestor as parent (maybe not needed)
            // This action will not move anything actually but the resulting tree will be proper
            if (!isTerminal(documentReference)) {
                List<SpaceReference> parentSpaces = documentReference.getSpaceReferences();
                if (parentSpaces.size() > 1) {
                    parentReference = new DocumentReference(SPACE_HOME_PAGE, parentSpaces.get(parentSpaces.size() - 2));
                }
            } else {
                parentReference = new DocumentReference(SPACE_HOME_PAGE, parentReference.getLastSpaceReference());
            }
        }

        return parentReference;
    }

    /**
     * Create the action concerning a specific document only.
     *  
     * @param documentReference the document to convert
     * @param parentAction the parent action
     * @param plan the plan
     *  
     * @return the action concerning this document only.
     */
    private MigrationAction createAction(DocumentReference documentReference, MigrationAction parentAction, 
            MigrationPlanTree plan)
    {
        MigrationAction action;

        // If the parent is not the top level action
        if (parentAction.getTargetDocument() != null) {
            if (isTerminal(documentReference)) {
                // The new parent space is created with the name of the original document, under the space of the parent
                // document.
                // [Space.Page, Path.To.Parent.WebHome] => [Path.To.Parent.Page.WebHome].
                // Not that the original space name is lost in the process.
                SpaceReference parentSpace = new SpaceReference(documentReference.getName(),
                        parentAction.getTargetDocument().getLastSpaceReference());
                DocumentReference targetDocument = new DocumentReference(SPACE_HOME_PAGE, parentSpace);
                action = MigrationAction.createInstance(documentReference, targetDocument, parentAction, plan);
            } else {
                // The document is already a WebHome, so we use the current space name under the space of the parent 
                // document.
                // [Space.WebHome, Path.To.Parent.WebHome] => [Path.To.Parent.Space.WebHome].
                SpaceReference spaceReference = new SpaceReference(documentReference.getLastSpaceReference().getName(),
                        parentAction.getTargetDocument().getLastSpaceReference());
                DocumentReference targetDocument = new DocumentReference(SPACE_HOME_PAGE, spaceReference);
                action = MigrationAction.createInstance(documentReference, targetDocument, parentAction, plan);
            }
        } else {
            // The parent is the top level action, ie. the document is orphan
            if (isTerminal(documentReference)) {
                // We only need to convert the document to nested
                // [Space.Page] => [Space.Page.WebHome].
                action = convertDocumentWithoutMove(documentReference, plan);
                parentAction.addChild(action);
            } else {
                // We have nothing to do!
                // [Space.WebHome] => [Space.WebHome].
                action = IdentityMigrationAction.createInstance(documentReference, parentAction, plan);
            }
        }

        return action;
    }

    /** 
     * @param documentReference the document to test
     * @return either or not the document is terminal, ie. its name is not "WebHome".
     */
    private boolean isTerminal(DocumentReference documentReference)
    {
        return !SPACE_HOME_PAGE.equals(documentReference.getName());
    }
}
