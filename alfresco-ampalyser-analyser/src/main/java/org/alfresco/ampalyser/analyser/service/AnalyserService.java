package org.alfresco.ampalyser.analyser.service;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.alfresco.ampalyser.analyser.checker.BeanOverwritingChecker.WHITELIST_BEAN_OVERRIDING;
import static org.alfresco.ampalyser.analyser.checker.BeanRestrictedClassesChecker.WHITELIST_BEAN_RESTRICTED_CLASSES;
import static org.alfresco.ampalyser.analyser.checker.Checker.ALFRESCO_VERSION;
import static org.alfresco.ampalyser.analyser.checker.FileOverwritingChecker.FILE_MAPPING_NAME;
import static org.alfresco.ampalyser.model.Resource.Type.FILE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.alfresco.ampalyser.analyser.comparators.WarComparatorService;
import org.alfresco.ampalyser.analyser.result.Conflict;
import org.alfresco.ampalyser.analyser.store.WarInventoryReportStore;
import org.alfresco.ampalyser.inventory.service.InventoryService;
import org.alfresco.ampalyser.model.InventoryReport;
import org.alfresco.ampalyser.model.Resource;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalyserService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyserService.class);

    public static final String EXTENSION_FILE_TYPE = "EXTENSION_FILE_TYPE";
    private static final String WHITELIST_BEAN_OVERRIDING_FILE = "/bean-overriding-whitelist.default.json";
    private static final String WHITELIST_BEAN_RESTRICTED_CLASSES_FILE = "/bean-restricted-classes-whitelist.default.json";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private WarInventoryReportStore warInventoryStore;

    @Autowired
    private WarComparatorService warComparatorService;
    
    @Autowired
    private AnalyserOutputService outputService;

    @Autowired
    private ObjectMapper objectMapper;

    public void analyse(final String ampPath, final SortedSet<String> alfrescoVersions,
        final String whitelistBeanOverridingPath, final String whitelistRestrictedClassesPath,
        final boolean verboseOutput)
    {
        // build the *ampInventoryReport*:
        final InventoryReport ampInventory = inventoryService.extractInventoryReport(ampPath);
        List<Properties> fileMappingFiles = findFileMappingFiles(ampPath, ampInventory.getResources().get(FILE));
        Set<String> beanOverridingWhitelist = loadWhitelistBeanOverriding(whitelistBeanOverridingPath);
        Set<String> beanRestrictedClassesWhitelist = loadWhitelistBeanRestrictedClasses(whitelistRestrictedClassesPath);

        final Map<String, List<Conflict>> conflictsPerWarVersion = alfrescoVersions
            .stream()
            .collect(toMap(identity(), v -> warComparatorService.findConflicts(
                ampInventory,
                warInventoryStore.retrieve(v),
                Map.of(
                    ALFRESCO_VERSION, v,
                    FILE_MAPPING_NAME, fileMappingFiles,
                    WHITELIST_BEAN_OVERRIDING, beanOverridingWhitelist,
                    WHITELIST_BEAN_RESTRICTED_CLASSES, beanRestrictedClassesWhitelist,
                    EXTENSION_FILE_TYPE, FileUtils.getExtension(ampPath)
                ))
            ));
        
        final Map<Conflict.Type, Map<String, Set<Conflict>>> conflictPerTypeAndResourceId = 
            groupByTypeAndResourceId(conflictsPerWarVersion);

        outputService.print(conflictPerTypeAndResourceId, verboseOutput);
    }

    static Map<Conflict.Type, Map<String, Set<Conflict>>> groupByTypeAndResourceId(
        Map<String, List<Conflict>> conflictsPerWarVersion)
    {
        return conflictsPerWarVersion
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(groupingBy(
                Conflict::getType, 
                TreeMap::new,
                groupingBy(
                    conflict -> conflict.getAmpResourceInConflict().getId(),
                    TreeMap::new,
                    toSet()
                )));
    }

    /**
     * Extracts the mapping options specified in the .amp as {@link List}s of {@link Properties}
     * @param ampPath The filepath of the .amp required to open the .zip file and read the file-mapping.properties file(s)
     * @param ampResources The .amp resources
     * @return
     */
    private static List<Properties> findFileMappingFiles(String ampPath, Collection<Resource> ampResources)
    {
        List<Properties> foundProperties = new ArrayList<>();

        // Look for "file-mapping.properties"
        List<Resource> mappingResources = ampResources.stream()
            .filter(fileResource -> (fileResource.getId() != null && fileResource.getId().contains(FILE_MAPPING_NAME)))
            .collect(toUnmodifiableList());

        // If no resource is found quickly return before unzipping and iterating through the whole amp (.zip) file
        if (mappingResources.size() < 1)
        {
            LOGGER.info(FILE_MAPPING_NAME + " was not found in the provided .amp. Continuing with default mapping.");
            return emptyList();
        }

        try
        {
            ZipFile zipFile = new ZipFile(ampPath);
            for (Resource resource : mappingResources)
            {
                // Trim the first slash when looking in the .zip
                ZipEntry entry = zipFile.getEntry(resource.getId().substring(1));
                if (FILE_MAPPING_NAME.equals(entry.getName()))
                {
                    InputStream is = zipFile.getInputStream(entry);
                    Properties properties = new Properties();
                    properties.load(is);
                    foundProperties.add(properties);
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to read from the " + FILE_MAPPING_NAME + " although it was found.");
            return emptyList();
        }

        return foundProperties;
    }

    /**
     * Reads and loads whitelist for the beans in a war that can be overridden when an .amp is applied
     *
     * @return a {@link Set} of the whitelisted beans (that can be overridden).
     */
    private Set<String> loadWhitelistBeanOverriding(String whitelistFilePath)
    {
        Set<String> whitelist = new HashSet<>();

        try
        {
            whitelist.addAll(objectMapper.readValue(
                getClass().getResourceAsStream(WHITELIST_BEAN_OVERRIDING_FILE),
                new TypeReference<>() {}));
        }
        catch (IOException ioe)
        {
            LOGGER.error("Failed to read DEFAULT Bean Overriding Whitelist file: " + WHITELIST_BEAN_OVERRIDING_FILE, ioe);
            throw new RuntimeException("Failed to read DEFAULT Bean Overriding Whitelist file: " + WHITELIST_BEAN_OVERRIDING_FILE, ioe);
        }


        if (whitelistFilePath == null)
        {
            return whitelist;
        }

        try
        {
            whitelist.addAll(objectMapper.readValue(
                new FileInputStream(whitelistFilePath),
                new TypeReference<>() {}));
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to read Bean Overriding Whitelist file: " + whitelistFilePath, e);
            throw new RuntimeException("Failed to read Bean Overriding Whitelist file: " + whitelistFilePath, e);
        }

        return whitelist;
    }

    /**
     * Reads and loads a class whitelist for the .amp beans from a .json file
     *
     * @return a {@link Set} of the whitelisted beans (that can be overridden).
     */
    private Set<String> loadWhitelistBeanRestrictedClasses(String whitelistRestrictedClassesPath)
    {
        Set<String> whitelist = new HashSet<>();

        try
        {
            whitelist.addAll(objectMapper.readValue(
                getClass().getResourceAsStream(WHITELIST_BEAN_RESTRICTED_CLASSES_FILE),
                new TypeReference<>() {}));
        }
        catch (IOException ioe)
        {
            LOGGER.error("Failed to read DEFAULT Bean Restricted Classes Whitelist file: " + WHITELIST_BEAN_RESTRICTED_CLASSES_FILE, ioe);
            throw new RuntimeException("Failed to read DEFAULT Bean Restricted Whitelist file: " + WHITELIST_BEAN_RESTRICTED_CLASSES_FILE, ioe);
        }

        if (whitelistRestrictedClassesPath == null)
        {
            return whitelist;
        }

        try
        {
            whitelist.addAll(objectMapper.readValue(
                new FileInputStream(whitelistRestrictedClassesPath),
                new TypeReference<>() {}));
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to read Bean Restricted Classes Whitelist file: " + whitelistRestrictedClassesPath, e);
            throw new RuntimeException("Failed to read Bean Restricted Classes Whitelist file: " + whitelistRestrictedClassesPath, e);
        }

        return whitelist;
    }
}
