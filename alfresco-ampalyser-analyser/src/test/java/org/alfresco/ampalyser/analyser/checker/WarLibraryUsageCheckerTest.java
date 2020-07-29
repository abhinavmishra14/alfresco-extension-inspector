package org.alfresco.ampalyser.analyser.checker;

import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.ampalyser.model.Resource.Type.CLASSPATH_ELEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.ampalyser.analyser.result.Conflict;
import org.alfresco.ampalyser.analyser.result.WarLibraryUsageConflict;
import org.alfresco.ampalyser.analyser.service.ConfigService;
import org.alfresco.ampalyser.analyser.service.ExtensionCodeAnalysisService;
import org.alfresco.ampalyser.analyser.service.ExtensionResourceInfoService;
import org.alfresco.ampalyser.model.ClasspathElementResource;
import org.alfresco.ampalyser.model.InventoryReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarLibraryUsageCheckerTest
{
    @Mock
    private ConfigService configService;
    @InjectMocks
    private ExtensionResourceInfoService extensionResourceInfoService = spy(ExtensionResourceInfoService.class);
    @Spy
    private ExtensionCodeAnalysisService extensionCodeAnalysisService;
    @InjectMocks
    private WarLibraryUsageChecker checker;

    @Test
    void processInternal()
    {
        // AMP Classpath elements
        {
            doReturn(List.of(
                res("/com/example/test/A1.nope", "white"),
                res("/com/example/test/A1.class", "white"),
                res("/com/example/test/A2.class", "white"),
                res("/com/example/test/A3.class", "white"),
                res("/com/example/test/A4.class", "white"),
                res("/com/example/test/A1.class", "black"),
                res("/com/example/test/A2.class", "black"),
                res("/com/example/test/A7.class", "black"),
                res("/com/example/test/A8.class", "black"),
                res("/com/example/test/A9.class", "black"),
                res("/com/example/test/C0.class", "color"),
                res("/com/example/test/C00.class", "color"),
                res("/com/example/test/C10.class", "color"),
                res("/com/example/test/C11.class", "color"),
                res("/com/example/test/C12.class", "color"),
                res("/com/example/test/C20.class", "color"),
                res("/com/example/test/C21.class", "color"),
                res("/com/example/test/C22.class", "color"),
                res("/com/example/test/C23.class", "color"),
                res("/com/example/test/C24.class", "color"),
                res("/com/example/test/C25.class", "color"),
                res("/com/example/test/C31.class", "color"),
                res("/com/example/test/C32.class", "color"),
                res("/com/example/test/C40.class", "color"),
                res("/com/example/test/C41.class", "color"),
                res("/com/example/test/C42.class", "color"),
                res("/com/example/test/C50.class", "color"),
                res("/com/example/test/C51.class", "color"),
                res("/com/example/test/C52.class", "color"),
                res("/com/example/test/C53.class", "color"),
                res("/com/example/test/C54.class", "color"),
                res("/com/example/test/C55.class", "color"),
                res("/com/example/test/C56.class", "color"),
                res("/com/example/test/C57.class", "color"),
                res("/com/example/test/C60.class", "color"),
                res("/com/example/test/C61.class", "color"),
                res("/com/example/test/C62.class", "color"),
                res("/com/example/test/C63.class", "color")
            )).when(configService).getExtensionResources(any());
        }

        final InventoryReport warInventory = new InventoryReport();
        {
            warInventory.setResources(Map.of(CLASSPATH_ELEMENT, List.of(
                res("/com/example/test/W10.class", "red"),
                res("/com/example/test/W11.class", "red"),
                res("/com/example/test/W12.class", "red"),
                res("/com/example/test/W13.class", "red"),
                res("/com/example/test/W14.class", "red"),

                res("/com/example/test/W13.class", "blue"),
                res("/com/example/test/W14.class", "blue"),
                res("/com/example/test/W15.class", "blue"),
                res("/com/example/test/W16.class", "blue"),
                res("/com/example/test/W17.class", "blue"),
                res("/com/example/test/W18.class", "blue"),
                res("/com/example/test/W19.class", "blue"),
                res("/com/example/test/W20.class", "blue"),
                res("/com/example/test/W21.class", "blue"),
                res("/com/example/test/W22.class", "blue"),

                res("/org/alfresco/W2.class", "alf"),
                res("/org/alfresco/W3.class", "alf"),
                res("/org/alfresco/W4.class", "alf"),

                res("/com/example/test/W10.nope", "alf"),
                res("/com/example/test/W11.nope", "alf"),
                res("/org/alfresco/W3.nope", "alf")
            )));
        }

        // AMP bytecode dependencies
        {
            doReturn(Map.ofEntries(
                // no dependencies
                entry("/com/example/test/C0.class", emptySet()),

                // dependencies not in AMP or WAR
                entry("/com/example/test/C11.class", Set.of("/nope/Nope.class")),

                // only AMP dependencies
                entry("/com/example/test/C21.class", Set.of("/com/example/test/A1.class")),
                entry("/com/example/test/C22.class", Set.of("/com/example/test/A1.class", "/nope/Nope.class")),
                entry("/com/example/test/C23.class", Set.of("/com/example/test/A1.class", "/com/example/test/A2.class")),
                entry("/com/example/test/C24.class", Set.of("/com/example/test/A1.class", "/com/example/test/A7.class")),
                entry("/com/example/test/C25.class", Set.of("/com/example/test/A8.class")),

                // Dependencies not in the WAR
                entry("/com/example/test/C31.class", Set.of("/org/alfresco/C0.class")),
                entry("/com/example/test/C32.class", Set.of("/org/alfresco/C0.class", "/org/alfresco/C1.class")),

                // Dependencies in the WAR
                entry("/com/example/test/C41.class", Set.of("/org/alfresco/w2.class")),
                entry("/com/example/test/C42.class", Set.of("/org/alfresco/w2.class", "/org/alfresco/W3.class")),

                entry("/com/example/test/C51.class", Set.of("/com/example/test/W10.class")),
                entry("/com/example/test/C52.class", Set.of("/com/example/test/W17.class")),

                entry("/com/example/test/C53.class", Set.of("/com/example/test/W13.class")),

                entry("/com/example/test/C54.class", Set.of("/com/example/test/W11.class", "/com/example/test/W15.class")),

                entry("/com/example/test/C55.class", Set.of("/com/example/test/W19.class", "/org/alfresco/W4.class")),
                entry("/com/example/test/C56.class", Set.of("/com/example/test/W20.class", "/some/Other.class")),

                entry("/com/example/test/A2.class", Set.of("/com/example/test/W21.class")),

                // Dependencies in AMP and WAR
                entry("/com/example/test/C61.class", Set.of("/com/example/test/A2.class", "/com/example/test/W22.class"))
            )).when(extensionCodeAnalysisService).retrieveDependenciesPerClass();
        }

        final Set<Conflict> result = checker.process(warInventory, "6.0.0").collect(toSet());

        final Set<Conflict> expected = Set.of(
            conflict(res("/com/example/test/C51.class", "color"), Set.of("/com/example/test/W10.class")),
            conflict(res("/com/example/test/C52.class", "color"), Set.of("/com/example/test/W17.class")),
            conflict(res("/com/example/test/C53.class", "color"), Set.of("/com/example/test/W13.class")),
            conflict(res("/com/example/test/C54.class", "color"), Set.of("/com/example/test/W11.class", "/com/example/test/W15.class")),
            conflict(res("/com/example/test/C55.class", "color"), Set.of("/com/example/test/W19.class")),
            conflict(res("/com/example/test/C56.class", "color"), Set.of("/com/example/test/W20.class")),
            conflict(res("/com/example/test/A2.class", "white"), Set.of("/com/example/test/W21.class")),
            conflict(res("/com/example/test/A2.class", "black"), Set.of("/com/example/test/W21.class")),
            conflict(res("/com/example/test/C61.class", "color"), Set.of("/com/example/test/W22.class"))
        );
        assertEquals(expected.size(), result.size());
        expected.forEach(c -> assertTrue(expected.contains(c)));
    }

    private static ClasspathElementResource res(String id, String definingObject)
    {
        return new ClasspathElementResource(id, definingObject);
    }

    private static WarLibraryUsageConflict conflict(ClasspathElementResource resource, Set<String> classes)
    {
        return new WarLibraryUsageConflict(resource, classes, "6.0.0");
    }
}