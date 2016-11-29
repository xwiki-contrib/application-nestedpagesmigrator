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
package org.xwiki.contrib.nestedpagesmigrator.internal.job;

/**
 * @version $Id: $
 */

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.internal.breakage.Breakage;
import org.xwiki.contrib.nestedpagesmigrator.internal.breakage.HierarchyBreakageDetector;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.PagesToTransformGetter;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;

/**
 * Job that computes a migration plan.
 *
 * @version $Id: $
 * @since 0.7
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(HierarchyBreakageDetectorJob.JOB_TYPE)
public class HierarchyBreakageDetectorJob extends AbstractJob<MigrationPlanRequest, HierarchyBreakageDetectorJobStatus>
{
    /**
     * The job type.
     */
    public static final String JOB_TYPE = "npmig.breakageDetector";

    @Inject
    private PagesToTransformGetter pagesToTransformGetter;

    @Inject
    private HierarchyBreakageDetector hierarchyBreakageDetector;

    @Override
    protected void runInternal() throws Exception
    {
        try {
            MigrationConfiguration configuration = request.getConfiguration();

            progressManager.pushLevelProgress(2, this);

            // Step 1: Detect pages
            progressManager.startStep(this);
            List<DocumentReference> pages =  pagesToTransformGetter.getPagesToConvert(configuration);

            // Step 2: Detect breakages
            progressManager.startStep(this);
            List<Breakage> breakages = hierarchyBreakageDetector.detectBreakage(pages);
            getStatus().setBreakages(breakages);

            progressManager.popLevelProgress(this);
        } catch (Exception e) {
            logger.error("Failed to detect the broken pages.", e);
            e.printStackTrace();
        }
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected HierarchyBreakageDetectorJobStatus createNewStatus(MigrationPlanRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new HierarchyBreakageDetectorJobStatus(request, currentJobStatus, this.observationManager,
                this.loggerManager);
    }
}
