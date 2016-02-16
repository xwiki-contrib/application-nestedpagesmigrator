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
package org.xwiki.contrib.nestedpagesmigrator.internal.preferences;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id: $
 * @since 0.3
 */
@Component(roles = PreferencesPropertiesGetter.class)
@Singleton
public class PreferencesPropertiesGetter
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    public Collection<String> getPreferencesProperties() throws MigrationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        Collection<String> properties = new ArrayList<>();

        try {
            BaseClass preferencesClass =
                xwiki.getXClass(new DocumentReference(context.getWikiId(), "XWiki", "XWikiPreferences"), context);
            for (PropertyClass property : preferencesClass.getEnabledProperties()) {
                properties.add(property.getName());
            }
        } catch (XWikiException e) {
            throw new MigrationException("Failed to get the list of preferences properties.", e);
        }

        return properties;
    }
}
