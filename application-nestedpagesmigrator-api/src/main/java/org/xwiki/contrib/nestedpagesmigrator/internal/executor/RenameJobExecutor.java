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
package org.xwiki.contrib.nestedpagesmigrator.internal.executor;

import java.lang.InterruptedException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.search.solr.internal.api.SolrIndexer;

/**
 * Execute the rename Job in the current thread.
 *
 * @version $Id: $
 * @since 0.4.2
 */
@Component(roles = RenameJobExecutor.class)
@Singleton
public class RenameJobExecutor
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private SolrIndexer solrIndexer;

    public void rename(DocumentReference origin, DocumentReference target, DocumentReference author,
            MigrationConfiguration configuration)
            throws ComponentLookupException
    {
        // Create a MoveRequest from the Refactoring API
        MoveRequest request = new MoveRequest();

        // Source, target
        request.setEntityReferences(Arrays.asList((EntityReference) origin));
        request.setDestination(target);

        // Configuration
        request.setAutoRedirect(configuration.isAddAutoRedirect());
        request.setCheckRights(false);
        request.setDeep(false);
        request.setInteractive(false);
        request.setUpdateLinks(true);
        request.setUserReference(author);
        request.setUpdateParentField(false); // we do it manually because of a bug

        // Create the job and run it
        Job job = componentManager.getInstance(Job.class, RefactoringJobs.RENAME);
        job.initialize(request);

        // Wait until SOLR finish indexing previously renamed page before running the job
        while (this.solrIndexer.getQueueSize() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        job.run();
    }
}
