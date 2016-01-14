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
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlan;
import org.xwiki.contrib.nestedpagesmigrator.NestedPagesMigrator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id: $
 */
@Component
@Singleton
public class DefaultNestedPagesMigrator implements NestedPagesMigrator
{
    @Inject
    private TerminalPagesGetter terminalPagesGetter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public MigrationPlan computeMigrationPlan(MigrationConfiguration configuration) throws MigrationException
    {
        List<DocumentReference> terminalDocs = terminalPagesGetter.getTerminalPages(configuration);
        MigrationPlan plan = new MigrationPlan();

        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        if (configuration.isDontMoveChildren()) {
            for (DocumentReference terminalDoc : terminalDocs) {
                convertDocumentWithoutMove(terminalDoc, plan);
            }
        } else {
            for (DocumentReference terminalDoc : terminalDocs) {
                convertDocumentAndParents(terminalDoc, plan, terminalDocs, context, xwiki);
            }
        }

        plan.sort();
        return plan;
    }

    private MigrationAction convertDocumentWithoutMove(DocumentReference terminalDoc, MigrationPlan plan)
    {
        SpaceReference parentSpace = new SpaceReference(terminalDoc.getName(), terminalDoc.getLastSpaceReference());
        DocumentReference targetDoc = new DocumentReference("WebHome", parentSpace);
        MigrationAction action = new MigrationAction(terminalDoc, targetDoc);
        plan.addAction(action);
        return action;
    }

    /**
     * @return the migration of the document
     */
    private MigrationAction convertDocumentAndParents(DocumentReference documentReference, MigrationPlan plan, 
            List<DocumentReference> concernedDocuments,
            XWikiContext context,
            XWiki xwiki) throws MigrationException
    {
        if (documentReference == null) {
            return plan.getTopLevelAction();
        }

        MigrationAction existingAction = plan.getActionAbout(documentReference);
        if (existingAction != null) {
            return existingAction;
        }
        
        if (!concernedDocuments.contains(documentReference)) {
            MigrationAction action = new MigrationAction(documentReference, documentReference);
            plan.getTopLevelAction().addChild(action);
            plan.addAction(action);
            return action;
        }

        // Get the document to know the parent
        XWikiDocument document;
        try {
            document = xwiki.getDocument(documentReference, context);
        } catch (XWikiException e) {
            logger.error("Failed to open the document [{}].", documentReference, e);
            return new MigrationAction(documentReference, documentReference);
        }
        
        // Not sure here:
        DocumentReference parentReference = document.getParentReference();
        if (parentReference == null) {
            parentReference = new DocumentReference("WebHome", documentReference.getLastSpaceReference());
            if (parentReference.equals(documentReference)) {
                parentReference = null;
            }
        }
        
        /*
        if (document.isNew()) {
            // The document might not exists
            // 2 strategies are possible: 
            // - just convert them to the top location
            // - create an empty parent
            return new MigrationAction(documentReference, documentReference);
        }*/

        MigrationAction parentAction = convertDocumentAndParents(parentReference, plan, concernedDocuments, 
                context, xwiki);
        MigrationAction action;

        if (parentAction.getTargetDocument() != null) {
            if ("WebHome".equals(documentReference.getName())) {
                
                SpaceReference spaceReference = new SpaceReference(documentReference.getLastSpaceReference().getName(),
                    parentAction.getTargetDocument().getLastSpaceReference());
                DocumentReference targetDocument = new DocumentReference("WebHome", spaceReference);
                action = new MigrationAction(documentReference, targetDocument);
                
            } else {
                SpaceReference parentSpace = new SpaceReference(documentReference.getName(),
                        parentAction.getTargetDocument().getLastSpaceReference());
                DocumentReference targetDocument = new DocumentReference("WebHome", parentSpace);
                action = new MigrationAction(documentReference, targetDocument);
            }
        } else {
            if ("WebHome".equals(documentReference.getName())) {
                action = new MigrationAction(documentReference, documentReference);
            } else {
                action = convertDocumentWithoutMove(documentReference, plan);
            }
        }

        plan.addAction(action);
        parentAction.addChild(action);

        return action;
    }

}
