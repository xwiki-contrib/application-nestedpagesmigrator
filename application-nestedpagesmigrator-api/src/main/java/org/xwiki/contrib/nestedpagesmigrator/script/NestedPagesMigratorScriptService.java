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
package org.xwiki.contrib.nestedpagesmigrator.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.internal.serializer.MigrationPlanSerializer;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.NestedPagesMigrator;
import org.xwiki.contrib.nestedpagesmigrator.script.internal.StatusAndLogSerializer;
import org.xwiki.job.Job;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Script service to create or execute migration plan via some jobs.
 *
 * @version $Id: $
 * @since 0.2
 */
@Named("nestedpagesmigrator")
@Singleton
@Component
public class NestedPagesMigratorScriptService implements ScriptService
{
    @Inject
    private NestedPagesMigrator nestedPagesMigrator;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;
    
    private void checkAdminAccess(WikiReference wikiReference) throws AccessDeniedException
    {
        // User need to be admin to run this application
        authorizationManager.checkAccess(Right.ADMIN, documentAccessBridge.getCurrentUserReference(), wikiReference);
    }

    /**
     * Start the creation of a migration plan in a job.
     *
     * @param configuration the migration configuration
     * @return the job handling the computation of the plan
     *
     * @throws MigrationException if error happens
     * @throws AccessDeniedException if the user have not the right to execute this method
     */
    public Job startMigrationPlanCreation(MigrationConfiguration configuration)
            throws MigrationException, AccessDeniedException
    {
        checkAdminAccess(configuration.getWikiReference());
        
        return nestedPagesMigrator.startMigrationPlanCreation(configuration);
    }

    /**
     * Create a new migration configuration.
     *
     * @param wikiId the id of the wiki where the migration will be done
     *
     * @return a new migration configuration for that wiki
     */
    public MigrationConfiguration newMigrationConfiguration(String wikiId)
    {
        return new MigrationConfiguration(new WikiReference(wikiId));
    }

    /**
     * @param wikiId the id of the wiki where the plan have been computed
     * @return a JSON-serialized version of the computed plan for the given wiki
     *
     * @throws AccessDeniedException if the user have not the right to execute this method
     */
    public String getSerializedPlan(String wikiId) throws AccessDeniedException
    {
        checkAdminAccess(new WikiReference(wikiId));
        
        return MigrationPlanSerializer.serialize(nestedPagesMigrator.getPlan(wikiId));
    }

    /**
     * Start the execution of a previously computed plan.
     *
     * @param configuration the configuration of the migration
     * @param serializedPlan the plan to execute, JSON-serialized
     * @return the job which executes the migration
     *
     * @throws AccessDeniedException if the current user has not ADMIN right on the wiki
     * @throws AccessDeniedException if the user have not the right to execute this method
     *
     * @since 0.8
     */
    public Job startMigration(MigrationConfiguration configuration, String serializedPlan)
            throws AccessDeniedException, MigrationException
    {
        checkAdminAccess(configuration.getWikiReference());


        MigrationPlanTree plan = nestedPagesMigrator.getPlan(configuration.getWikiReference().getName());

        return nestedPagesMigrator.startMigration(plan, configuration);
    }

    /**
     * Display the status and the logs of the migration action as JSON.
     *
     * @param wikiId id of the wiki
     * @param action "createmigrationplan" or "executemigrationplan"
     * @return the status and the logs as JSON
     *
     * @throws AccessDeniedException if the user have not the right to execute this method
     *
     * @since 0.4.3
     */
    public String getStatusAndLogs(String wikiId, String action) throws AccessDeniedException
    {
        checkAdminAccess(new WikiReference(wikiId));

        return StatusAndLogSerializer.serialize(nestedPagesMigrator.getStatus(wikiId, action));
    }

    /**
     * Remove the plan from the memory.
     *
     * @param wikiId id of the wiki where there is the plan to clear
     *
     * @since 0.4.2
     */
    public void clearPlan(String wikiId) throws AccessDeniedException
    {
        checkAdminAccess(new WikiReference(wikiId));

        nestedPagesMigrator.clearPlan(wikiId);
    }

    /**
     * Start the breakage detection in a job.
     *
     * @param configuration the migration configuration
     * @return the job handling the detection
     *
     * @throws MigrationException if error happens
     * @throws AccessDeniedException if the user have not the right to execute this method
     *
     * @since 0.7
     */
    public Job startBreakageDetection(MigrationConfiguration configuration) throws AccessDeniedException,
            MigrationException
    {
        checkAdminAccess(configuration.getWikiReference());

        return nestedPagesMigrator.startBreakageDetection(configuration);
    }

    /**
     * @param wikiId the id of the wiki where the plan have been computed
     * @return a JSON-serialized version of the breakage list for the given wiki
     *
     * @throws AccessDeniedException if the user have not the right to execute this method
     *
     * @since 0.7
     */
    public String getSerializedBreakages(String wikiId) throws AccessDeniedException
    {
        checkAdminAccess(new WikiReference(wikiId));

        return MigrationPlanSerializer.serialize(nestedPagesMigrator.getBreakages(wikiId));
    }
}
