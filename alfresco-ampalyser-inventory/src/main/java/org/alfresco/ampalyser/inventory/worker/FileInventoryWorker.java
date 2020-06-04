/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser.inventory.worker;

import static org.alfresco.ampalyser.commons.InventoryUtils.isFromJar;

import java.util.List;
import java.util.zip.ZipEntry;

import org.alfresco.ampalyser.model.FileResource;
import org.alfresco.ampalyser.model.Resource;
import org.alfresco.ampalyser.inventory.EntryProcessor;
import org.springframework.stereotype.Component;

@Component
public class FileInventoryWorker implements InventoryWorker
{
    public FileInventoryWorker(EntryProcessor processor)
    {
        processor.attach(this);
    }

    @Override
    public List<Resource> processInternal(ZipEntry zipEntry, byte[] data, String definingObject)
    {
        return List.of(new FileResource(zipEntry.getName(), definingObject));
    }

    @Override
    public Resource.Type getType()
    {
        return Resource.Type.FILE;
    }

    @Override
    public boolean canProcessEntry(ZipEntry entry, String definingObject)
    {
        return !(entry == null || entry.isDirectory() || isFromJar(entry, definingObject));
    }
}
