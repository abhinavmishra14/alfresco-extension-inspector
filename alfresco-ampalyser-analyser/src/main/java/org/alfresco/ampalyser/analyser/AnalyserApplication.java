/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.ampalyser.analyser;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import org.alfresco.ampalyser.analyser.service.AnalyserService;
import org.alfresco.ampalyser.analyser.store.AlfrescoTargetVersionParser;
import org.alfresco.ampalyser.inventory.AlfrescoWarInventory;
import org.alfresco.ampalyser.inventory.InventoryApplication;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@SpringBootApplication
@ComponentScan(
    basePackages = {"org.alfresco.ampalyser.inventory", "org.alfresco.ampalyser.analyser"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {AlfrescoWarInventory.class, InventoryApplication.class })
    })
public class AnalyserApplication implements ApplicationRunner, ExitCodeGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(AnalyserApplication.class);

    private static final int EXIT_CODE_EXCEPTION = 1;

    @Autowired
    private AlfrescoTargetVersionParser alfrescoTargetVersionParser;
    @Autowired
    private AnalyserService analyserService;

    private int exitCode = 0;

    public static void main(String[] args)
    {
        System.exit(SpringApplication.exit(SpringApplication.run(AnalyserApplication.class, args)));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        if (args.getNonOptionArgs().isEmpty())
        {
            logger.error("Missing extension file.");
            printUsage();
            setExceptionExitCode();
            return;
        }

        final String extensionPath = args.getNonOptionArgs().get(0);
        if (!isExtensionValid(extensionPath))
        {
            logger.error("The extension file is not valid.");
            printUsage();
            setExceptionExitCode();
            return;
        }

        // TODO: Improve argument retrieval and validation via ACS-355
        final List<String> beanOverridingWhitelistPaths = args.getOptionValues("whitelistBeanOverriding");
        String whitelistBeanOverridingPath = beanOverridingWhitelistPaths == null || beanOverridingWhitelistPaths.isEmpty() ? null : beanOverridingWhitelistPaths.get(0);
        if (beanOverridingWhitelistPaths != null && beanOverridingWhitelistPaths.size() > 1)
        {
            logger.error("Multiple Bean Overriding Whitelists provided.");
            printUsage();
            setExceptionExitCode();
            return;
        }

        final List<String> beanRestrictedClassWhitelistPaths = args.getOptionValues("whitelistBeanRestrictedClasses");
        String whitelistRestrictedClassesPath = beanRestrictedClassWhitelistPaths == null || beanRestrictedClassWhitelistPaths.isEmpty() ? null : beanRestrictedClassWhitelistPaths.get(0);
        if (beanRestrictedClassWhitelistPaths != null && beanRestrictedClassWhitelistPaths.size() > 1)
        {
            logger.error("Multiple Bean Restricted Classes Whitelists provided.");
            printUsage();
            setExceptionExitCode();
            return;
        }


        final SortedSet<String> versions = alfrescoTargetVersionParser.parse(args.getOptionValues("target"));
        if (versions.isEmpty())
        {
            logger.error("The target ACS version was not recognised.");
            printUsage();
            setExceptionExitCode();
            return;
        }

        analyserService.analyse(extensionPath, versions, whitelistBeanOverridingPath,
            whitelistRestrictedClassesPath, args.containsOption("verbose"));
    }

    private static boolean isExtensionValid(final String extensionPath)
    {
        return new File(extensionPath).exists() &&
               (FilenameUtils.getExtension(extensionPath).equalsIgnoreCase("amp") ||
                FilenameUtils.getExtension(extensionPath).equalsIgnoreCase("jar"));
    }

    private static void printUsage()
    {
        System.out.println("Usage:");
        System.out.println("java -jar alfresco-ampalyser-analyser.jar <extension-filename> [--target=6.1.0[-7.0.0]] "
            + "[--whitelistBeanOverriding=/path/to/bean_overriding_whitelist.json] "
            + "[--whitelistBeanRestrictedClasses=/path/to/bean_restricted_classes_whitelist.json] " 
            + "[--verbose]");
    }

    @Bean
    public ObjectMapper objectMapper()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objectMapper;
    }

    @Bean
    ExitCodeExceptionMapper exitCodeToExceptionMapper()
    {
        return exception -> {
            // Set specific exit codes based on the exception type

            // Default exit code
            return 1;
        };
    }

    /**
     * @return the code 1 if an exception occurs. Otherwise, on a clean exit, it
     * provides 0 as the exit code.
     */
    @Override
    public int getExitCode()
    {
        return exitCode;
    }

    /**
     * Set the exit code to default exception exit code.
	 */
	private void setExceptionExitCode()
	{
		this.exitCode = EXIT_CODE_EXCEPTION;
	}
}
