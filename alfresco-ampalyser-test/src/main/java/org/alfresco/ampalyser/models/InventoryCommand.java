/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser.models;

import java.util.List;

public class InventoryCommand
{
        private List<String> commandOptions;

        public InventoryCommand(List<String> commandOptions)
        {
                this.commandOptions = commandOptions;
        }

        public List<String> getCommandOptions()
        {
                return commandOptions;
        }

        public void addCommandOptions(List<String> commandOptions)
        {
                this.commandOptions.addAll(commandOptions);
        }

        @Override
        public String toString()
        {
                return String.join(" ", commandOptions);
        }
}