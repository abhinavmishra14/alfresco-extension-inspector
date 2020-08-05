/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser.analyser.printers;

import static java.lang.String.valueOf;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.alfresco.ampalyser.analyser.result.Conflict.Type.BEAN_RESTRICTED_CLASS;
import static org.alfresco.ampalyser.analyser.service.PrintingService.printTable;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.alfresco.ampalyser.analyser.result.Conflict;
import org.alfresco.ampalyser.analyser.store.WarInventoryReportStore;
import org.alfresco.ampalyser.model.BeanResource;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class BeanRestrictedClassConflictPrinter implements ConflictPrinter
{
    private static final String HEADER =
        "Found beans that instantiate internal classes." + lineSeparator() + "The "
            + "following beans instantiate classes from Alfresco or 3rd party libraries which must "
            + "not be instantiated by custom beans:";
    private static final String EXTENSION_RESOURCE_ID = "Extension Bean Resource ID instantiating Restricted Class";

    @Autowired
    private WarInventoryReportStore store;

    @Override
    public SortedSet<String> retrieveAllKnownVersions()
    {
        return store.allKnownVersions();
    }

    @Override
    public String getHeader()
    {
        return HEADER;
    }

    @Override
    public Conflict.Type getConflictType()
    {
        return BEAN_RESTRICTED_CLASS;
    }

    @Override
    public void printVerboseOutput(Set<Conflict> conflictSet)
    {
        String[][] data =  conflictSet
            .stream()
            .collect(groupingBy(conflict -> conflict.getAmpResourceInConflict().getId(),
                TreeMap::new,
                toUnmodifiableSet()))
            .entrySet().stream()
            .map(entry -> List.of(
                entry.getKey(),
                entry.getValue().iterator().next().getAmpResourceInConflict().getDefiningObject(),
                ((BeanResource)entry.getValue().iterator().next().getAmpResourceInConflict()).getBeanClass(),
                joinWarVersions(entry.getValue()),
                valueOf(entry.getValue().size())))
            .map(rowAsList -> rowAsList.toArray(new String[0]))
            .toArray(String[][]::new);

        data = ArrayUtils.insert(0, data, new String[][] {
            new String[] { EXTENSION_RESOURCE_ID, EXTENSION_DEFINING_OBJECT, RESTRICTED_CLASS,
                WAR_VERSION, TOTAL } });
        
        printTable(data);
    }

    @Override
    public void print(Set<Conflict> conflictSet)
    {
        String[][] data = conflictSet.stream()
            .map(conflict -> List.of(
                    conflict.getAmpResourceInConflict().getId(),
                    ((BeanResource)conflict.getAmpResourceInConflict()).getBeanClass()))
            .distinct()
            .map(rowAsList -> rowAsList.toArray(new String[0]))
            .toArray(String[][]::new);

        data = ArrayUtils.insert(0, data,
            new String[][] { new String[] { EXTENSION_RESOURCE_ID, RESTRICTED_CLASS } });
        printTable(data);
    }
}
