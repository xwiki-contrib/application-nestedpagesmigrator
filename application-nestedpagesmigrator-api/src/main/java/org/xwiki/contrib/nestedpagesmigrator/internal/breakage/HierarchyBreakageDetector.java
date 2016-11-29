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
package org.xwiki.contrib.nestedpagesmigrator.internal.breakage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Detect the documents that would lost their "parent" if they are not migrated.
 *
 * Example: "Animals.Cat" having the parent field set to "Species.Mammal", will lose this information
 * if the migration is not performed, since its hierarchical parent is "Animals.WebHome".
 *
 * @version $Id: $
 * @since 0.7
 */
@Component(roles = HierarchyBreakageDetector.class)
@Singleton
public class HierarchyBreakageDetector
{
    private static final String SPACE_HOME = "WebHome";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private Logger logger;

    public List<Breakage> detectBreakage(List<DocumentReference> documentsToLookAt)
    {
        progressManager.pushLevelProgress(documentsToLookAt.size(), this);
        List<Breakage> results = new ArrayList<>();
        for (DocumentReference reference : documentsToLookAt) {
            progressManager.startStep(this);
            DocumentReference locationParent = computeLocationParent(reference);
            DocumentReference actualParent = getActualParent(reference);
            if (locationParent != null && !locationParent.equals(actualParent)) {
                results.add(new Breakage(reference, locationParent, actualParent));
            }
        }
        progressManager.popLevelProgress(this);
        return results;
    }

    private DocumentReference computeLocationParent(DocumentReference document)
    {
        // Case 1: A.B, parent is A.WebHome
        if (!SPACE_HOME.equals(document.getName())) {
            return new DocumentReference(SPACE_HOME, new SpaceReference(document.getParent()));
        }

        EntityReference grandParent = document.getParent().getParent();

        // Case 2: A.B.WebHome, parent is A.WebHome
        if (grandParent.getType() == EntityType.SPACE) {
            return new DocumentReference(SPACE_HOME, new SpaceReference(grandParent));
        }

        // Case 3: B.WebHome, parent is Main.WebHome
        if (grandParent.getType() == EntityType.WIKI) {
            return new DocumentReference(SPACE_HOME, new SpaceReference("Main", new WikiReference(grandParent)));
        }

        // Other case should not exist
        return null;
    }

    private DocumentReference getActualParent(DocumentReference documentReference)
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            return xwiki.getDocument(documentReference, context).getParentReference();
        } catch (Exception e) {
            logger.warn("Failed to load the parent of [{}].", documentReference, e);
            return null;
        }
    }
}
