/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.extension_inspector.analyser.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.alfresco.extension_inspector.model.InventoryReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Lucian Tuca
 */
@Component
public class JSONInventoryParser implements InventoryParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONInventoryParser.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public InventoryReport parseReport(String path)
    {
        try
        {
            // TODO: ACS-76 depending on how deep we want to dive, we can read line by line and only parse the types that we're interested in to save memory and time.
            return objectMapper.readValue(new File(path), InventoryReport.class);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to read inventory file: " + path, e);
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    public InventoryReport parseReport(final InputStream is)
    {
        try
        {
            return objectMapper.readValue(is, InventoryReport.class);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to read inventory stream", e);
            throw new RuntimeException("Failed to read inventory stream: ", e);
        }
    }
}