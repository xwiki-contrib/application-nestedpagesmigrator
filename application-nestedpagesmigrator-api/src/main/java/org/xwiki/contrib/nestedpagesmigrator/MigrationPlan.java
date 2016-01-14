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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id: $
 */
public class MigrationPlan
{
    private Map<DocumentReference, MigrationAction> actions = new HashMap<>();
    
    private MigrationAction topLevelAction = new MigrationAction(null, null);
    
    public Map<DocumentReference, MigrationAction> getActions()
    {
        return actions;
    }
    
    public boolean isEmpty()
    {
        return actions.isEmpty();
    }
    
    public boolean isAlreadyComputed(DocumentReference documentReference)
    {
        return actions.containsKey(documentReference);    
    }
    
    public void addAction(MigrationAction action)
    {
        actions.put(action.getSourceDocument(), action);
    }
    
    public MigrationAction getActionAbout(DocumentReference documentReference)
    {
        return actions.get(documentReference);
    }

    public MigrationAction getTopLevelAction()
    {
        return topLevelAction;
    }
    
    public void sort()
    {
        for (MigrationAction action : actions.values()) {
            Collections.sort(action.getChildren(), new Comparator<MigrationAction>()
            {
                @Override
                public int compare(MigrationAction a1, MigrationAction a2)
                {
                    return a1.getTargetDocument().toString().compareTo(a2.getTargetDocument().toString());
                }
            });
        }
        
    }
}
