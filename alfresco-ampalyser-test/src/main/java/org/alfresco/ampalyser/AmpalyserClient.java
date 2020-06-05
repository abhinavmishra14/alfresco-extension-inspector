/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser;

import org.alfresco.ampalyser.command.CommandExecutor;
import org.alfresco.ampalyser.command.CommandImpl;
import org.alfresco.ampalyser.command.CommandReceiver;
import org.alfresco.ampalyser.model.Resource;
import org.alfresco.ampalyser.models.CommandOutput;
import org.alfresco.ampalyser.models.InventoryCommand;
import org.alfresco.ampalyser.models.InventoryTestReport;
import org.alfresco.ampalyser.util.JsonInventoryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class AmpalyserClient
{
        @Autowired
        private InventoryCommand cmd;
        @Autowired
        private CommandReceiver commReceiver;
        @Autowired
        private CommandExecutor executor;
        @Autowired
        private JsonInventoryParser jsonInventory;

        public CommandOutput runInventoryAnalyserCommand(List<String> cmdOptions)
        {
                // Add additional inventory command options
                cmd.addCommandOptions(cmdOptions);
                CommandImpl invCmd = new CommandImpl(commReceiver, cmd);

                System.out.println("Running command: " + cmd.toString());
                CommandOutput cmdOut = executor.execute(invCmd);

                return cmdOut;
        }

        public Resource retrieveInventoryResource(Resource.Type resourceType, String resourceId, File jsonReport)
        {
                InventoryTestReport inventoryTestReport = jsonInventory.getInventoryReportFromJson(jsonReport);

                return inventoryTestReport.getResource(resourceType, resourceId);
        }

        public List<Resource> retrieveInventoryResources(Resource.Type resourceType, File jsonReport)
        {
                InventoryTestReport inventoryTestReport = jsonInventory.getInventoryReportFromJson(jsonReport);

                return inventoryTestReport.getResources(resourceType);
        }
}
