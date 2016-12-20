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
package org.xwiki.contrib.nestedpagesmigrator.testframework;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * A dependency-free document resolver used by tests.
 * Can be used as a component to inject, or directly.
 * (The code is a crappy, but good enough for test purpose).
 *
 * @version $Id: $
 * @since 0.8
 */
public class BasicDocumentReferenceResolver implements DocumentReferenceResolver<String>
{
    @Override
    public DocumentReference resolve(String fullName, Object... objects)
    {
        if (StringUtils.isBlank(fullName)) {
            return null;
        }

        int index = fullName.lastIndexOf(".");
        String page = fullName.substring(index + 1);
        String spacePart = fullName.substring(0, index);

        String wiki = "xwiki";

        if (spacePart.contains(":")) {
            int wikiPart = spacePart.indexOf(":");
            wiki = spacePart.substring(0, wikiPart);
            spacePart = spacePart.substring(wikiPart + 1);
        }

        String spaces[] = spacePart.split("\\.");
        List<String> spaceList = new ArrayList<>();
        for (String space : spaces) {
            spaceList.add(space);
        }
        return new DocumentReference(wiki, spaceList, page);
    }
}
