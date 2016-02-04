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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id: $
 */
public class MigrationPlanTree
{
    private Map<DocumentReference, MigrationAction> actions = new HashMap<>();

    private Map<DocumentReference, MigrationAction> actionsByTarget = new HashMap<>();
    
    private List<MigrationPlanTreeListener> listeners = new ArrayList<>();
    
    /**
     * Top level action: the root of the tree, but do not represent a real action.
     */ 
    private MigrationAction topLevelAction = new MigrationAction(null, null);
    
    public Map<DocumentReference, MigrationAction> getActions()
    {
        return actions;
    }
    
    public void addAction(MigrationAction action) throws MigrationException
    {
        if (actions.containsKey(action.getSourceDocument())) {
            throw new MigrationException(String.format("An action concerning [%s] already exists.",
                    action.getSourceDocument()));
        }
        if (actionsByTarget.containsKey(action.getTargetDocument())) {
            throw new MigrationException(String.format("An action with target [%s] already exists.",
                    action.getTargetDocument()));
        }
        actions.put(action.getSourceDocument(), action);
        actionsByTarget.put(action.getTargetDocument(), action);
        
        for (MigrationPlanTreeListener listener : listeners) {
            listener.actionAdded(this, action);
        }
    }
    
    public MigrationAction getActionAbout(DocumentReference documentReference)
    {
        MigrationAction action = actions.get(documentReference);
        return action != null ? action : getActionWithTarget(documentReference);
    }

    public MigrationAction getActionWithTarget(DocumentReference documentReference)
    {
        return actionsByTarget.get(documentReference);
    }

    public MigrationAction getTopLevelAction()
    {
        return topLevelAction;
    }
    
    public void sort()
    {
        for (MigrationAction action : actions.values()) {
            Collections.sort(action.getChildren());
        }
        Collections.sort(topLevelAction.getChildren());
    }
    
    public void addListener(MigrationPlanTreeListener listener)
    {
        listeners.add(listener);
    }
}
