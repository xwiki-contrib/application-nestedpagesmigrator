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
package org.xwiki.contrib.nestedpagesmigrator;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.nestedpagesmigrator.internal.breakage.Breakage;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;

/**
 * @version $Id: $
 */
@Role
public interface NestedPagesMigrator
{
    Job startMigrationPlanCreation(MigrationConfiguration configuration) throws MigrationException;

    Job startPreferencesMigrationPlanCreation(MigrationPlanTree plan, MigrationConfiguration configuration)
            throws MigrationException;

    MigrationPlanTree getPlan(String wikiId);

    Job startMigration(MigrationPlanTree plan, MigrationConfiguration configuration) throws MigrationException;

    /**
     * @since 0.4.2
     */
    JobStatus getStatus(String wikiId, String action);

    /**
     * @since 0.4.2
     */
    void clearPlan(String wikiId);

    /**
     * @since 0.7
     */
    Job startBreakageDetection(MigrationConfiguration configuration) throws MigrationException;

    /**
     * @since 0.7
     */
    List<Breakage> getBreakages(String wikiId);
}
