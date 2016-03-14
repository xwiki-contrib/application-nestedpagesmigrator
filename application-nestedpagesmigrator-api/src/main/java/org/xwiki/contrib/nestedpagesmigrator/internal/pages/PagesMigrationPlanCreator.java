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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTreeListener;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.logging.Message;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A migration plan creator which computes ALL required actions to convert a wiki to Nested Pages. Not thread-safe!
 * Note: the rights and the preferences are not handled by this component.
 *
 * @version $Id: $
 */
@Component(roles = PagesMigrationPlanCreator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PagesMigrationPlanCreator implements Initializable, MigrationPlanTreeListener
{
    private static final String SPACE_HOME_PAGE = "WebHome";
    
    @Inject
    private PagesToTransformGetter pagesToTransformGetter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private Logger logger;
    
    private XWikiContext context;
    
    private XWiki xwiki;
    
    private MigrationPlanTree plan;

    private List<DocumentReference> concernedDocuments;

    private Set<DocumentReference> visitedDocuments = new HashSet<>();

    private MigrationConfiguration configuration;

    @Override
    public void initialize() throws InitializationException
    {
        context = contextProvider.get();
        xwiki = context.getWiki();
    } 

    /**
     * Compute all migration actions needed to convert a wiki to Nested Pages.
     *
     * @param configuration the configuration
     * @return the migration plan tree
     * @throws MigrationException if problems occur
     */
    public MigrationPlanTree computeMigrationPlan(MigrationConfiguration configuration) throws MigrationException
    {
        this.configuration = configuration;

        // Start
        progressManager.pushLevelProgress(3, this);

        // Get the pages to convert
        progressManager.startStep(this, new Message("Get the pages to convert"));
        concernedDocuments = pagesToTransformGetter.getPagesToConvert(configuration);
        
        // Compute the plan
        progressManager.startStep(this, new Message("Compute the migration plan"));
        plan = new MigrationPlanTree();
        plan.addListener(this);
        
        progressManager.pushLevelProgress(concernedDocuments.size(), this);
        progressManager.startStep(this);
        if (configuration.isDontMoveChildren()) {
            for (DocumentReference terminalDoc : concernedDocuments) {
                convertDocumentAndItsParentWithoutMove(terminalDoc);
            }
        } else {
            for (DocumentReference documentReference : concernedDocuments) {
                convertDocumentAndParents(documentReference);
            }
        }
        progressManager.popLevelProgress(this);

        // A sorted migration plan tree is more user-friendly.
        progressManager.startStep(this, new Message("Sort the plan"));
        plan.sort();

        // Free the memory
        concernedDocuments.clear();
        visitedDocuments.clear();

        // End
        this.progressManager.popLevelProgress(this);
        return plan;
    }

    private void convertDocumentAndItsParentWithoutMove(DocumentReference terminalDoc) throws MigrationException
    {
        MigrationAction action = convertDocumentWithoutMove(terminalDoc);
        // Create an identity action for the the parent to have a nice tree at the end.
        MigrationAction parentAction = convertParentWithoutMove(terminalDoc);
        parentAction.addChild(action);
    }

    private MigrationAction convertParentWithoutMove(DocumentReference originalDocument) throws MigrationException
    {
        DocumentReference spaceHomeReference = new DocumentReference(SPACE_HOME_PAGE,
                originalDocument.getLastSpaceReference());
        
        MigrationAction parentAction = plan.getActionAbout(spaceHomeReference);
        if (parentAction == null) {
            parentAction = IdentityMigrationAction.createInstance(spaceHomeReference, plan.getTopLevelAction(), plan);
        }
        return parentAction;
    }

    /**
     * Convert a terminal document to nested pages without trying to move it under its parents.
     *  
     * @param terminalDoc the document to convert
     *  
     * @return the action concerning this document
     */
    private MigrationAction convertDocumentWithoutMove(DocumentReference terminalDoc) 
            throws MigrationException
    {
        SpaceReference parentSpace = new SpaceReference(terminalDoc.getName(), terminalDoc.getLastSpaceReference());
        DocumentReference targetDoc = computeFreeTarget(terminalDoc, parentSpace, null);
        
        return MigrationAction.createInstance(terminalDoc, targetDoc, plan);
    }

    /**
     * Convert a document and all its parents to Nested Pages.
     *  
     * @param documentReference the document to convert
     *  
     * @return the migration of the document
     */
    private MigrationAction convertDocumentAndParents(DocumentReference documentReference) throws MigrationException
    {
        // A planned action concerning this document might have been created already. We avoid recomputing the 
        // conversion by returning the existing action, if there is any.
        MigrationAction existingAction = plan.getActionAbout(documentReference);
        if (existingAction != null) {
            return existingAction;
        }

        // Since the user might have configured some exclusions, we verify that this document is contained in the list
        // of documents to convert. Note: the document might not exist. In that case, it is good to compute a plan
        // for it (even not applied) to compute a good path for its children.
        if (!concernedDocuments.contains(documentReference) && xwiki.exists(documentReference, context)) {
            // Otherwise, we create an "identity" action: it does nothing but it will added to the plan so that action
            // won't be recomputed afterwards.
            // Note that this action is added as child of the top-level action, because we want to have it in the plan 
            // tree.
            return IdentityMigrationAction.createInstance(documentReference, plan.getTopLevelAction(), plan);
        }

        // If the document have already been visited, we are probably experiencing a cyclic parent/child relationship:
        // A -> B -> C -> A
        if (!visitedDocuments.add(documentReference)) {
            // In that case, we put the document at the top level
            return plan.getTopLevelAction();
        }

        // Now we are sure that the document needs to be converted, so let's start the real job.
        // --

        // Get the parent reference
        DocumentReference parentReference;
        try {
            parentReference = getParent(documentReference);
        } catch (XWikiException e) {
            logger.warn("Failed to open the document [{}].", documentReference, e);
            // Don't fail the migration just because of that, return an identity action instead.
            return IdentityMigrationAction.createInstance(documentReference, plan.getTopLevelAction(), plan);
        }

        // Get the action concerning the parent (or the top level action if the document is orphan). Because a plan 
        // concerning the parents must have been prepared before we compute the plan for the current document.
        MigrationAction parentAction = parentReference != null ?
                convertDocumentAndParents(parentReference) : plan.getTopLevelAction();

        // Now, create the action for the current document
        return createAction(documentReference, parentAction);
    }

    /**
     * Get the parent of a document.
     *  
     * @param documentReference the document
     * @return the parent document, or null if the document is and remains orphan
     *  
     * @throws XWikiException if the document cannot be loaded
     */
    private DocumentReference getParent(DocumentReference documentReference) throws XWikiException
    {
        // To read the "parent" field of the document, we need to load the document
        XWikiDocument document = xwiki.getDocument(documentReference, context);
        DocumentReference parentReference = document.getParentReference();

        // The parent field might not be filled. In that case, the document is actually an orphan.
        if (parentReference == null) {
            // What should we do ?
            // Best effort: use the space ancestor as parent.
            // [Space.Page, no parent] => [Space.Page.WebHome, Space.WebHome]
            if (isTerminal(documentReference)) {
                parentReference = new DocumentReference(SPACE_HOME_PAGE, documentReference.getLastSpaceReference());
            } else {
                List<SpaceReference> parentSpaces = documentReference.getSpaceReferences();
                if (parentSpaces.size() > 1) {
                    // [PageA.PageB.WebHome, no parent] => [PageA.PageB.WebHome, PageA.WebHome]
                    parentReference = new DocumentReference(SPACE_HOME_PAGE, parentSpaces.get(parentSpaces.size() - 2));
                }
                // Otherwise, we let the document orphan
                // [Space.WebHome, no parent] => [Space.WebHome, no parent].
            }
        } else if (!parentReference.getWikiReference().equals(configuration.getWikiReference())) {
            // The parent is on an other wiki
            parentReference = null;
        }

        return parentReference;
    }

    /**
     * Create the action concerning a specific document only.
     *  
     * @param documentReference the document to convert
     * @param parentAction the parent action
     *  
     * @return the action concerning this document only.
     */
    private MigrationAction createAction(DocumentReference documentReference, MigrationAction parentAction)
            throws MigrationException
    {
        MigrationAction action;

        // If the parent is not the top level action, ie: the parent exists.
        if (parentAction.getTargetDocument() != null) {
            if (isTerminal(documentReference)) {
                action = createActionForTerminalDocument(documentReference, parentAction);
            } else {
                // The document is already a WebHome, so we use the current space name under the space of the parent 
                // document.
                // [Space.WebHome, Path.To.Parent.WebHome] => [Path.To.Parent.Space.WebHome].
                SpaceReference spaceReference = new SpaceReference(documentReference.getLastSpaceReference().getName(),
                        parentAction.getTargetDocument().getLastSpaceReference());
                DocumentReference targetDocument = computeFreeTarget(documentReference, spaceReference, null);
                
                action = MigrationAction.createInstance(documentReference, targetDocument, parentAction, plan);
            }
        } else {
            // The parent is the top level action, ie. the document is orphan
            if (isTerminal(documentReference)) {
                // We only need to convert the document to nested
                // [Space.Page] => [Space.Page.WebHome].
                action = convertDocumentWithoutMove(documentReference);
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
     * Create the action concerning a specific document only, which is terminal.
     *
     * @param documentReference the document to convert
     * @param parentAction the parent action
     *
     * @return the action concerning this document only.
     * @throws MigrationException
     */
    private MigrationAction createActionForTerminalDocument(DocumentReference documentReference,
            MigrationAction parentAction) throws MigrationException
    {
        // The new parent space is created with the name of the original document, under the space of the parent
        // document.
        // [Space.Page, Path.To.Parent.WebHome] => [Path.To.Parent.Page.WebHome].
        // Not that the original space name is lost in the process.
        SpaceReference parentSpace = new SpaceReference(documentReference.getName(),
                parentAction.getTargetDocument().getLastSpaceReference());
        DocumentReference targetDocument = computeFreeTarget(documentReference, parentSpace, parentAction);

        // Because of computeFreeTarget(), the target document might have a different level of nesting. In that case,
        // the parent is a virtual document that must be present in the plan.
        // [Dramas.List, Movies.WebHome] -> [Movies.Dramas.List, Movies.Dramas.WebHome]
        // --> Movies.Dramas.WebHome must be created!
        if (!targetDocument.getLastSpaceReference().getParent().equals(
                parentAction.getTargetDocument().getLastSpaceReference())) {
            parentAction = convertDocumentAndParents(new DocumentReference(SPACE_HOME_PAGE, 
                    (SpaceReference) targetDocument.getLastSpaceReference().getParent()));
        }

        return MigrationAction.createInstance(documentReference, targetDocument, parentAction, plan);
    }

    /**
     * Verify that the target document is not already used by a previous action or already existing in the wiki.
     */
    private boolean isTargetFree(DocumentReference documentReference, DocumentReference targetDocument)
    {
        MigrationAction conflictingAction = plan.getActionWithTarget(targetDocument);
        // Note: it's ok to have a target document that exists if the target document is the source document too.
        // ie: if the action do not move the document (identity action).
        return conflictingAction == null
                && (!xwiki.exists(targetDocument, context) || documentReference.equals(targetDocument));
    }

    /**
     * Generate a target document that is not already used by a previous action or already existing in the wiki.
     */
    private DocumentReference computeFreeTarget(DocumentReference documentReference,
            SpaceReference parentSpace, MigrationAction parentAction)
    {
        DocumentReference targetDocument = new DocumentReference(SPACE_HOME_PAGE, parentSpace);
        int iteration = 0;
        while (!isTargetFree(documentReference, targetDocument)) {
            SpaceReference newParentSpace = parentSpace;
            
            // Best effort: we could add an interesting information by adding the name of the space of the old
            // document in front of the document name.
            // ie: [Dramas.List, Movies.WebHome] -> [Movies.Dramas.List] instead of [Movies.List_2].
            // But it make sense only if the space name is not the same than the target parent.
            // ie: we avoid having [Movies.Dramas.Dramas.List] as target.
            if (parentAction != null && !documentReference.getLastSpaceReference().getName().equals(
                    parentAction.getTargetDocument().getLastSpaceReference().getName())) {
                newParentSpace = new SpaceReference(documentReference.getName(),
                        new SpaceReference(documentReference.getLastSpaceReference().getName(),
                                parentAction.getTargetDocument().getLastSpaceReference()));
            }
            
            // This new name could be used already, so we add a number prefix
            if (iteration++ > 0) {
                String spaceName = newParentSpace.getName() + "_" + iteration;
                newParentSpace = new SpaceReference(spaceName, newParentSpace.getParent());
            }
            
            // Create the new reference
            targetDocument = new DocumentReference(SPACE_HOME_PAGE, newParentSpace);
        }
        return targetDocument;
    }

    /** 
     * @param documentReference the document to test
     * @return either or not the document is terminal, ie. its name is not "WebHome".
     */
    private boolean isTerminal(DocumentReference documentReference)
    {
        return !SPACE_HOME_PAGE.equals(documentReference.getName());
    }

    @Override
    public void actionAdded(MigrationPlanTree plan, MigrationAction action)
    {   
        if (concernedDocuments.contains(action.getSourceDocument())) {
            progressManager.startStep(this);
        }
    }
}
