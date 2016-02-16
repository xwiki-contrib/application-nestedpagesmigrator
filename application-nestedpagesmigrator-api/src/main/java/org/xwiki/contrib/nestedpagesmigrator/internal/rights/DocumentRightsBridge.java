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
package org.xwiki.contrib.nestedpagesmigrator.internal.rights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.nestedpagesmigrator.MigrationException;
import org.xwiki.contrib.nestedpagesmigrator.Right;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id: $
 * @since 0.3
 */
@Component(roles = DocumentRightsBridge.class)
@Singleton
public class DocumentRightsBridge
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    public Collection<Right> getRights(DocumentReference document) throws MigrationException
    {
        DocumentReference classReference = new DocumentReference(document.getWikiReference().getName(), "XWiki",
                "XWikiGlobalRights");

        Collection<Right> rights = new ArrayList<>();

        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(document, context);
            Collection<BaseObject> rightsObjects = doc.getXObjects(classReference);
            if (rightsObjects != null) {
                for (BaseObject obj : rightsObjects) {
                    if (obj == null) {
                        continue;
                    }
                    Collection<String> groups = getValues("groups", obj);
                    Collection<String> users  = getValues("users", obj);
                    Collection<String> levels = getValues("levels", obj);
                    boolean allow = obj.getIntValue("allow", 1) == 1;
                    parseRight(users, levels, allow, true, document.getWikiReference(), rights);
                    parseRight(groups, levels, allow, false, document.getWikiReference(), rights);
                }
            }
        } catch (XWikiException e) {
            throw new MigrationException(String.format("Failed to get the objects of the document [%s].", document), e);
        }

        return rights;
    }

    private void parseRight(Collection<String> targets, Collection<String> levels, boolean allow, boolean isTargetUsers,
            WikiReference wikiReference, Collection<Right> rights)
    {
        for (String target : targets) {
            DocumentReference targetRef = documentReferenceResolver.resolve(target, wikiReference);
            for (String level : levels) {
                if (isTargetUsers) {
                    rights.add(new Right(targetRef, null, level, allow));
                } else {
                    rights.add(new Right(null, targetRef, level, allow));
                }
            }
        }
    }

    public Collection<String> getValues(String property, BaseObject object)
    {
        String value = object.getLargeStringValue(property);
        if (value == null) {
            return Collections.emptyList();
        }
        Collection<String> results = new ArrayList<>();
        for (String v : value.split(",")) {
            if (StringUtils.isNotBlank(v)) {
                results.add(v);
            }
        }
        return results;
    }
}
