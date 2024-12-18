/*
 * Copyright 2021 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.extension_inspector.analyser.checker;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.alfresco.extension_inspector.model.Resource.Type.CLASSPATH_ELEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.extension_inspector.analyser.result.ClasspathConflict;
import org.alfresco.extension_inspector.analyser.result.Conflict;
import org.alfresco.extension_inspector.analyser.service.ConfigService;
import org.alfresco.extension_inspector.analyser.service.ExtensionResourceInfoService;
import org.alfresco.extension_inspector.model.ClasspathElementResource;
import org.alfresco.extension_inspector.model.InventoryReport;
import org.alfresco.extension_inspector.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClasspathConflictsCheckerTest
{
    @Mock
    private ConfigService configService;
    @InjectMocks
    private ExtensionResourceInfoService extensionResourceInfoService = spy(ExtensionResourceInfoService.class);
    @InjectMocks
    private ClasspathConflictsChecker checker;

    @Test
    void testProcessInternal()
    {
        final Set<Resource> ampResources = Set.of(
            new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-west-2.0.jar"),
            new ClasspathElementResource("/org/alfresco/Blue.class", "/WEB-INF/lib/lib-east-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/White.class", "/WEB-INF/lib/lib-south-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/Purple.class", "/WEB-INF/lib/lib-south-1.0.jar")
        );
        final Set<Resource> warResources = Set.of(
            new ClasspathElementResource("/org/alfresco/Red.class", "/WEB-INF/lib/lib-west-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-west-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-east-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/Blue.class", "/WEB-INF/lib/lib-east-1.0.jar"),
            new ClasspathElementResource("/org/alfresco/White.class", "/WEB-INF/lib/lib-north-1.0.jar")
        );

        final List<Conflict> expectedResult = unmodifiableList(List.of(
            new ClasspathConflict(
                new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-west-2.0.jar"),
                new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-west-1.0.jar"),
                "6.0.0"
            ),

            new ClasspathConflict(
                new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-west-2.0.jar"),
                new ClasspathElementResource("/org/alfresco/Black.class", "/WEB-INF/lib/lib-east-1.0.jar"),
                "6.0.0"
            ),

            new ClasspathConflict(
                new ClasspathElementResource("/org/alfresco/Blue.class", "/WEB-INF/lib/lib-east-1.0.jar"),
                new ClasspathElementResource("/org/alfresco/Blue.class", "/WEB-INF/lib/lib-east-1.0.jar"),
                "6.0.0"
            ),

            new ClasspathConflict(
                new ClasspathElementResource("/org/alfresco/White.class", "/WEB-INF/lib/lib-south-1.0.jar"),
                new ClasspathElementResource("/org/alfresco/White.class", "/WEB-INF/lib/lib-north-1.0.jar"),
                "6.0.0"
            )
        ));

        InventoryReport warReport = new InventoryReport();
        warReport.setResources(Map.of(CLASSPATH_ELEMENT, warResources));

        doReturn(ampResources).when(configService).getExtensionResources(eq(CLASSPATH_ELEMENT));

        final List<Conflict> actualResult = checker.processInternal(warReport, "6.0.0").collect(toList());
        assertEquals(expectedResult.size(), actualResult.size());
        assertTrue(actualResult.containsAll(expectedResult));
    }
}