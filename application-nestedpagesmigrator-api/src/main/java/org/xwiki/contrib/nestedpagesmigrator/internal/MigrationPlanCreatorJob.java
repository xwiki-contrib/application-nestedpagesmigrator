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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;

/**
 * @version $Id: $
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(MigrationPlanCreatorJob.JOB_TYPE)
public class MigrationPlanCreatorJob extends AbstractJob<MigrationPlanRequest, MigrationPlanCreatorJobStatus>
{
    /**
     * The job type.
     */
    public static final String JOB_TYPE = "npmig";
    
    @Inject
    private ComponentManager componentManager;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private RightsMigrationPlanCreator rightsMigrationPlanCreator;

    @Override
    protected void runInternal() throws Exception
    {
        progressManager.pushLevelProgress(3, this);

        // Step 1: convert pages
        progressManager.startStep(this);
        PagesMigrationPlanCreator pagesMigrationPlanCreator
                = componentManager.getInstance(PagesMigrationPlanCreator.class);
        MigrationPlanTree plan = pagesMigrationPlanCreator.computeMigrationPlan(request.getConfiguration());

        // Step 2: convert preferences
        progressManager.startStep(this);
        PreferencesMigrationPlanCreator preferencesMigrationPlanCreator
                = componentManager.getInstance(PreferencesMigrationPlanCreator.class);
        preferencesMigrationPlanCreator.convertPreferences(plan, request.getConfiguration());

        // Step 3: convert rights
        progressManager.startStep(this);
        rightsMigrationPlanCreator.convertRights(plan, request.getConfiguration());

        // End
        getStatus().setPlan(plan);
        progressManager.popLevelProgress(this);
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected MigrationPlanCreatorJobStatus createNewStatus(MigrationPlanRequest request)
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return new MigrationPlanCreatorJobStatus(request, currentJobStatus, this.observationManager,
                this.loggerManager);
    }
}
