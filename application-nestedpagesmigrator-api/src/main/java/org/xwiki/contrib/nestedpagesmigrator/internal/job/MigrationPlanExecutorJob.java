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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.nestedpagesmigrator.internal.executor.MigrationPlanExecutor;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;

/**
 * @version $Id: $
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(MigrationPlanExecutorJob.JOB_TYPE)
public class MigrationPlanExecutorJob extends AbstractJob<MigrationPlanExecutorRequest, DefaultJobStatus>
{
    /**
     * The job type.
     */
    public static final String JOB_TYPE = "npmig.executor";

    @Override
    protected void runInternal() throws Exception
    {
        MigrationPlanExecutorRequest request = getRequest();
        MigrationPlanExecutor executor = componentManager.getInstance(MigrationPlanExecutor.class);
        executor.performMigration(request.getPlan(), request.getConfiguration());
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }
}
