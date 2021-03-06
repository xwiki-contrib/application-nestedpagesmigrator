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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.NestedPagesMigrator;
import org.xwiki.contrib.nestedpagesmigrator.internal.breakage.Breakage;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.HierarchyBreakageDetectorJob;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.HierarchyBreakageDetectorJobStatus;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.MigrationPlanCreatorJob;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.MigrationPlanCreatorJobStatus;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.MigrationPlanExecutorJob;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.MigrationPlanExecutorRequest;
import org.xwiki.contrib.nestedpagesmigrator.internal.job.MigrationPlanRequest;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;

/**
 * Default implementation of {@link NestedPagesMigrator}.
 *
 * @version $Id: $
 */
@Component
@Singleton
public class DefaultNestedPagesMigrator implements NestedPagesMigrator
{
    public static final String CREATE_PLAN = "createmigrationplan";

    public static final String EXECUTE_PLAN = "executemigrationplan";

    public static final String BREAKAGE_DETECTION = "breakagedetection";

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private JobStatusStore jobStatusStore;

    @Override
    public Job startMigrationPlanCreation(MigrationConfiguration configuration) throws MigrationException
    {
        try {
            MigrationPlanRequest migrationPlanRequest = new MigrationPlanRequest();
            migrationPlanRequest.setId(getJobId(configuration.getWikiReference().getName(), CREATE_PLAN));
            migrationPlanRequest.setConfiguration(configuration);
            return jobExecutor.execute(MigrationPlanCreatorJob.JOB_TYPE, migrationPlanRequest);
        } catch (JobException e) {
            throw new MigrationException("Failed to create a migration plan.", e);
        }
    }

    @Override
    public Job startPreferencesMigrationPlanCreation(MigrationPlanTree plan, MigrationConfiguration configuration)
            throws MigrationException
    {
        try {
            MigrationPlanRequest migrationPlanRequest = new MigrationPlanRequest();
            migrationPlanRequest.setId(getJobId(configuration.getWikiReference().getName(), CREATE_PLAN));
            migrationPlanRequest.setConfiguration(configuration);
            migrationPlanRequest.setPlan(plan);
            plan.clearPreferences();
            return jobExecutor.execute(MigrationPlanCreatorJob.JOB_TYPE, migrationPlanRequest);
        } catch (JobException e) {
            throw new MigrationException("Failed to create a migration plan.", e);
        }
    }

    @Override
    public MigrationPlanTree getPlan(String wikiId)
    {
        MigrationPlanCreatorJobStatus jobStatus = (MigrationPlanCreatorJobStatus) getStatus(wikiId, CREATE_PLAN);
        return jobStatus.getPlan();
    }

    @Override
    public Job startMigration(MigrationPlanTree plan, MigrationConfiguration configuration) throws MigrationException
    {
        try {
            MigrationPlanExecutorRequest request = new MigrationPlanExecutorRequest();
            request.setId(getJobId(configuration.getWikiReference().getName(), EXECUTE_PLAN));
            request.setConfiguration(configuration);
            request.setPlan(plan);
            return jobExecutor.execute(MigrationPlanExecutorJob.JOB_TYPE, request);
        } catch (JobException e) {
            throw new MigrationException("Failed to execute the migration plan.", e);
        }
    }

    @Override
    public JobStatus getStatus(String wikiId, String action)
    {
        JobStatus jobStatus;

        List<String> jobId = getJobId(wikiId, action);

        Job job = jobExecutor.getJob(jobId);
        if (job != null) {
            jobStatus = job.getStatus();
        } else {
            jobStatus = jobStatusStore.getJobStatus(jobId);
        }

        return jobStatus;
    }

    @Override
    public void clearPlan(String wikiId)
    {
        MigrationPlanCreatorJobStatus jobStatus = (MigrationPlanCreatorJobStatus) getStatus(wikiId, CREATE_PLAN);
        jobStatus.setPlan(null);
    }

    @Override
    public Job startBreakageDetection(MigrationConfiguration configuration) throws MigrationException
    {
        try {
            MigrationPlanRequest request = new MigrationPlanRequest();
            request.setId(getJobId(configuration.getWikiReference().getName(), BREAKAGE_DETECTION));
            request.setConfiguration(configuration);
            return jobExecutor.execute(HierarchyBreakageDetectorJob.JOB_TYPE, request);
        } catch (JobException e) {
            throw new MigrationException("Failed to execute the breakage detection.", e);
        }
    }

    @Override
    public List<Breakage> getBreakages(String wikiId)
    {
        HierarchyBreakageDetectorJobStatus jobStatus =
                (HierarchyBreakageDetectorJobStatus) getStatus(wikiId, BREAKAGE_DETECTION);
        return jobStatus.getBreakages();
    }

    private List<String> getJobId(String wikiId, String action)
    {
        // One job per wiki
        return Arrays.asList(MigrationPlanCreatorJob.JOB_TYPE, action, wikiId);
    }
}
