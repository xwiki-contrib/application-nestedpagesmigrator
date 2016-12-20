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
package org.xwiki.contrib.nestedpagesmigrator.internal.serializer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.nestedpagesmigrator.MigrationPlanTree;
import org.xwiki.contrib.nestedpagesmigrator.testframework.BasicDocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id: $
 */
public class MigrationPlanDeserializerTest
{
    @Rule
    public MockitoComponentMockingRule<MigrationPlanDeserializer> mocker =
            new MockitoComponentMockingRule<>(MigrationPlanDeserializer.class);

    private DocumentReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        resolver = new BasicDocumentReferenceResolver();
        mocker.registerComponent(DocumentReferenceResolver.TYPE_STRING, resolver);
    }

    @Test
    public void test() throws Exception
    {
        String example = IOUtils.toString(getClass().getResourceAsStream("/plan.json"));

        MigrationPlanTree plan = mocker.getComponentUnderTest().deserialize(example);

        String result = MigrationPlanSerializer.serialize(plan);

        assertEquals(example, result);

    }
}
