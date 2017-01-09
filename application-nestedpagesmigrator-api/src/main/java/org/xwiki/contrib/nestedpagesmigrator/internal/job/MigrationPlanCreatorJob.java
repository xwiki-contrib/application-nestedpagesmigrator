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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.internal.pages.PagesMigrationPlanCreator;
import org.xwiki.contrib.nestedpagesmigrator.internal.preferences.PreferencesMigrationPlanCreator;
import org.xwiki.contrib.nestedpagesmigrator.internal.rights.RightsMigrationPlanCreator;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;

/**
 * Job that computes a migration plan.
 *
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
    private RightsMigrationPlanCreator rightsMigrationPlanCreator;

    @Override
    protected void runInternal() throws Exception
    {
        try {
            MigrationPlanTree plan = request.getPlan();
            MigrationConfiguration configuration = request.getConfiguration();

            if (plan == null) {
                plan = createFullPlan();
            } else {
                convertPreferences(plan, configuration);
            }

            // End
            getStatus().setPlan(plan);
            progressManager.popLevelProgress(this);
            logger.info("Plan is computed.");
        } catch (Exception e) {
            logger.error("Failed to compute the migration plan.", e);
            e.printStackTrace();
        }
    }

    private MigrationPlanTree createFullPlan()
            throws org.xwiki.component.manager.ComponentLookupException, MigrationException
    {
        MigrationConfiguration configuration = request.getConfiguration();

        // Announce the number of steps
        int numberOfSteps = 1;
        if (configuration.isConvertPreferences()) {
            numberOfSteps++;
        }
        if (configuration.isConvertRights()) {
            numberOfSteps++;
        }
        progressManager.pushLevelProgress(numberOfSteps, this);

        // Step 1: convert pages
        MigrationPlanTree plan = convertPages(configuration);

        // Step 2: convert preferences
        if (configuration.isConvertPreferences()) {
            convertPreferences(plan, configuration);
        }

        // Step 3: convert rights
        if (configuration.isConvertRights()) {
            convertRights(plan);
        }

        return plan;
    }

    private MigrationPlanTree convertPages(MigrationConfiguration configuration)
            throws org.xwiki.component.manager.ComponentLookupException, MigrationException
    {
        MigrationPlanTree plan;
        progressManager.startStep(this);
        logger.info("Compute the new page hierarchy.");
        PagesMigrationPlanCreator pagesMigrationPlanCreator
                = componentManager.getInstance(PagesMigrationPlanCreator.class);
        plan = pagesMigrationPlanCreator.computeMigrationPlan(configuration);
        return plan;
    }

    private void convertRights(MigrationPlanTree plan) throws MigrationException
    {
        progressManager.startStep(this);
        logger.info("Compute the new page rights.");
        rightsMigrationPlanCreator.convertRights(plan);
    }

    private void convertPreferences(MigrationPlanTree plan, MigrationConfiguration configuration)
            throws org.xwiki.component.manager.ComponentLookupException, MigrationException
    {
        progressManager.startStep(this);
        logger.info("Compute the new page preferences.");
        PreferencesMigrationPlanCreator preferencesMigrationPlanCreator
                = componentManager.getInstance(PreferencesMigrationPlanCreator.class);
        preferencesMigrationPlanCreator.convertPreferences(plan, configuration);
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
