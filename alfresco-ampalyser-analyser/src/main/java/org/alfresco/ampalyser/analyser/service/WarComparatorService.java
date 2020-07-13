/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.ampalyser.analyser.service;

import java.util.List;
import java.util.stream.Stream;

import org.alfresco.ampalyser.analyser.checker.Checker;
import org.alfresco.ampalyser.analyser.result.Conflict;
import org.alfresco.ampalyser.model.InventoryReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WarComparatorService
{
    @Autowired
    private List<Checker> checkers;

    public Stream<Conflict> findConflicts(final InventoryReport warInventory, final String alfrescoVersion)
    {
        // Call all checkers and return the found list of conflicts
        return checkers
            .stream()
            //todo enable parallel streams and check for performance improvements
            //.parallelStream()
            .flatMap(c -> c.process(warInventory, alfrescoVersion));
    }
}
