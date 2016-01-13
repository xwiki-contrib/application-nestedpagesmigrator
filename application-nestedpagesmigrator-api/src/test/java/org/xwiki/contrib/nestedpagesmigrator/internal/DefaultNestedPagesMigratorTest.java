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

import java.util.Iterator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationAction;
import org.xwiki.contrib.nestedpagesmigrator.MigrationConfiguration;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlan;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertNotEquals;

/**
 * @version $Id: $
 */
public class DefaultNestedPagesMigratorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultNestedPagesMigrator> mocker =
            new MockitoComponentMockingRule<>(DefaultNestedPagesMigrator.class);

    @Test
    public void verifyTerminalPagesAreConverted() throws Exception
    {
        MigrationPlan plan = mocker.getComponentUnderTest().computeMigrationPlan(
                new MigrationConfiguration(new WikiReference("wiki")));

    }
    
    @Test
    public void verifyMigrationActionsSorted() throws Exception
    {
        MigrationPlan plan = mocker.getComponentUnderTest().computeMigrationPlan(
                new MigrationConfiguration(new WikiReference("wiki")));
        verifyMigrationActionsAreSorted(plan);
    }
    
    private void verifyMigrationActionsAreSorted(MigrationPlan plan) throws Exception
    {
        if (plan.isEmpty()) {
            return;
        }
        
        Iterator<MigrationAction> it = plan.getActions().iterator();
        MigrationAction previousAction = it.next();
        while (it.hasNext()) {
            MigrationAction action = it.next();
            if (previousAction.getLevelOfNesting() > action.getLevelOfNesting()) {
                throw new Exception("Migration plan is not sorted!");
            }
        }
    }
    
    private void verifyMigrationsActionsAreUnique(MigrationPlan plan) throws Exception
    {
        List<MigrationAction> actions = plan.getActions();
        for (int i = 1; i < actions.size(); ++i) {
            MigrationAction action = actions.get(i);
            for (int j = 0; j < i; ++j) {
                MigrationAction otherAction = actions.get(j);
                assertNotEquals(action, otherAction);
                assertNotEquals(action.getSourceDocument(), otherAction.getSourceDocument());
                assertNotEquals(action.getTargetDocument(), otherAction.getTargetDocument());
            }
        }
    }
}
