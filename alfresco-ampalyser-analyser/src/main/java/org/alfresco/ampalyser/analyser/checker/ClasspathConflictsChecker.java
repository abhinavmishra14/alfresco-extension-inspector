/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.ampalyser.analyser.checker;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.alfresco.ampalyser.model.Resource.Type.CLASSPATH_ELEMENT;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Map;

import org.alfresco.ampalyser.analyser.result.ClasspathConflict;
import org.alfresco.ampalyser.analyser.result.Conflict;
import org.alfresco.ampalyser.model.InventoryReport;
import org.springframework.stereotype.Component;

@Component
public class ClasspathConflictsChecker implements Checker
{
    @Override
    public List<Conflict> processInternal(final InventoryReport ampInventory, final InventoryReport warInventory, Map<String, Object> extraInfo)
    {
        return ampInventory.getResources().getOrDefault(CLASSPATH_ELEMENT, emptyList())
            .stream()
            .flatMap(ar -> warInventory.getResources().getOrDefault(CLASSPATH_ELEMENT, emptyList())
                .stream()
                .filter(wr -> wr.getId().equals(ar.getId()))
                .map(wr -> new ClasspathConflict(ar, wr, (String) extraInfo.get(ALFRESCO_VERSION))))
            .collect(toUnmodifiableList());
    }

    @Override
    public boolean canProcess(final InventoryReport ampInventory, final InventoryReport warInventory, Map<String, Object> extraInfo)
    {
        return !isEmpty(ampInventory.getResources().get(CLASSPATH_ELEMENT)) &&
            !isEmpty(warInventory.getResources().get(CLASSPATH_ELEMENT));
    }
}
