/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.extension_inspector.inventory.worker;

import static java.util.Collections.singleton;
import static org.alfresco.extension_inspector.commons.InventoryUtils.isFromExtension;
import static org.alfresco.extension_inspector.commons.InventoryUtils.isFromJar;

import java.util.Set;
import java.util.zip.ZipEntry;

import org.alfresco.extension_inspector.model.ClasspathElementResource;
import org.alfresco.extension_inspector.model.Resource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathElementInventoryWorker implements InventoryWorker
{
    public static final String WEB_INF_CLASSES = "WEB-INF/classes/";

    @Override
    public Set<Resource> processInternal(ZipEntry zipEntry, byte[] data, String definingObject)
    {
        return processInternal(zipEntry, definingObject);
    }

    @Override
    public Resource.Type getType()
    {
        return Resource.Type.CLASSPATH_ELEMENT;
    }

    @Override
    public boolean canProcessEntry(ZipEntry entry, String definingObject)
    {
        return !(entry == null || definingObject == null) &&
            !entry.isDirectory() &&
            (entry.getName().startsWith(WEB_INF_CLASSES)
                || isFromExtension(entry)
                || isFromJar(entry, definingObject));
    }

    private Set<Resource> processInternal(ZipEntry zipEntry, String definingObject)
    {
        String resourceName = zipEntry.getName();
        if (resourceName.startsWith(WEB_INF_CLASSES))
        {
            resourceName = resourceName.substring(WEB_INF_CLASSES.length());
        }
        return singleton(new ClasspathElementResource("/" + resourceName, "/" + definingObject));
    }
}