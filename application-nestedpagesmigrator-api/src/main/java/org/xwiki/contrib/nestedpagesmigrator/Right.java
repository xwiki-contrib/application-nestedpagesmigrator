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

import java.io.Serializable;

import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id: $
 */
public class Right implements Serializable
{
    private DocumentReference user;

    private DocumentReference group;

    private String[] levels;

    private boolean value;

    public Right(DocumentReference user, DocumentReference group, String[] levels, boolean value)
    {
        this.user = user;
        this.group = group;
        this.levels = levels;
        this.value = value;
    }

    public DocumentReference getUser()
    {
        return user;
    }

    public void setUser(DocumentReference user)
    {
        this.user = user;
    }

    public DocumentReference getGroup()
    {
        return group;
    }

    public void setGroup(DocumentReference group)
    {
        this.group = group;
    }

    public String[] getLevels()
    {
        return levels;
    }

    public void setLevels(String[] levels)
    {
        this.levels = levels;
    }

    public boolean isAllow()
    {
        return value;
    }

    public void setValue(boolean value)
    {
        this.value = value;
    }
}
