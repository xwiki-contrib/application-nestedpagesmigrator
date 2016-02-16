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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.NestedPagesMigrator;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanSerializer;
import org.xwiki.job.Job;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
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
    private Provider<XWikiContext> xcontextProvider;
    
    private void checkAdminAccess(WikiReference wikiReference) throws AccessDeniedException
    {
        // User need to be admin to run this application
        XWikiContext xcontext = xcontextProvider.get();
        authorizationManager.checkAccess(Right.ADMIN, xcontext.getUserReference(), wikiReference);
    }
    
    public Job startMigrationPlanCreation(MigrationConfiguration configuration)
            throws MigrationException, AccessDeniedException
    {
        checkAdminAccess(configuration.getWikiReference());
        
        return nestedPagesMigrator.startMigrationPlanCreation(configuration);
    }
    
    public MigrationConfiguration newMigrationConfiguration(String wikiId)
    {
        return new MigrationConfiguration(new WikiReference(wikiId));
    }
    
    public String getSerializedPlan(String wikiId) throws AccessDeniedException
    {
        checkAdminAccess(new WikiReference(wikiId));
        
        return MigrationPlanSerializer.serialize(nestedPagesMigrator.getPlan(wikiId));
    }
    
}
