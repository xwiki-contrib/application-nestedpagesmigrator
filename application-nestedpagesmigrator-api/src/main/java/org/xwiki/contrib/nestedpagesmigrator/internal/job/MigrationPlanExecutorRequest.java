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

import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.job.AbstractRequest;

/**
 * @version $Id: $
 */
public class MigrationPlanExecutorRequest extends AbstractRequest
{
    private static final String PROPERTY_PREFIX = "npmig.executor.";

    private static final String PROPERTY_CONF = PROPERTY_PREFIX + "configuration";

    private static final String PROPERTY_PLAN = PROPERTY_PREFIX + "plan";
    
    public void setConfiguration(MigrationConfiguration configuration)
    {
          setProperty(PROPERTY_CONF, configuration);
    }
    
    public MigrationConfiguration getConfiguration()
    {
        return (MigrationConfiguration) getProperty(PROPERTY_CONF);
    }

    public void setPlan(MigrationPlanTree plan)
    {
        setProperty(PROPERTY_PLAN, plan);
    }

    public MigrationPlanTree getPlan()
    {
        return (MigrationPlanTree) getProperty(PROPERTY_PLAN);
    }
}
