/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.ampalyser.analyser.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.alfresco.ampalyser.model.Resource.Type.BEAN;
import static org.alfresco.ampalyser.model.Resource.Type.CLASSPATH_ELEMENT;
import static org.alfresco.ampalyser.model.Resource.Type.FILE;

import java.util.Map;
import java.util.Set;

import org.alfresco.ampalyser.model.BeanResource;
import org.alfresco.ampalyser.model.ClasspathElementResource;
import org.alfresco.ampalyser.model.FileResource;
import org.alfresco.ampalyser.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This Bean caches extension (amp/jar) information generated by processing
 * the extension Inventory Report. It does not keep the report, it keeps
 * only data in the format needed by the Checkers.
 * <p/>
 * The extension is static/immutable, therefore we can afford to process
 * information about it only once and cache the result for subsequent uses.
 * <p/>
 * Some Checkers need the same info about an Extension. But even if
 * they need different info, they'll still need it multiple times (once
 * for each WAR version).
 *
 * @author Cezar Leahu
 * @author Lucian Tuca
 */
@Component
public class ExtensionResourceInfoService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResourceInfoService.class);

    private static final String ORG_ALFRESCO_PREFIX = "org.alfresco";

    @Autowired
    private ConfigService configService;

    private Map<String, Set<BeanResource>> beanOverridesById;
    private Map<String, Set<ClasspathElementResource>> classpathElementsById;
    private Map<String, FileResource> filesByDestination;
    private Set<BeanResource> beansOfAlfrescoTypes;

    /**
     * Compile a filtered map of bean Resources by ID.
     * The same bean can be declared in multiple context files,
     * therefore there might be multiple bean resources with the same id.
     *
     * @return
     */
    public Map<String, Set<BeanResource>> retrieveBeanOverridesById()
    {
        if (beanOverridesById == null)
        {
            final Set<String> allowedList = configService.getBeanOverrideAllowedList();

            beanOverridesById = configService
                .getExtensionResources(BEAN)
                .stream()
                .filter(r -> !allowedList.contains(r.getId()))
                .map(r -> (BeanResource) r)
                .collect(groupingBy(Resource::getId, toUnmodifiableSet()));
        }
        return beanOverridesById;
    }

    /**
     * Return the extension classpath elements grouped by their ID. Classpath elements
     * with the same ID is a common occurrence, as the same class (package+name) is often
     * defined in multiple (different) libraries.
     * <p/>
     * The format of the dependency/class entries (ids) is: "/package/path/ClassName.class"
     *
     * @return
     */
    public Map<String, Set<ClasspathElementResource>> retrieveClasspathElementsById()
    {
        if (classpathElementsById == null)
        {
            classpathElementsById = configService
                .getExtensionResources(CLASSPATH_ELEMENT)
                .stream()
                .map(r -> (ClasspathElementResource) r)
                .filter(r -> !r.getId().toLowerCase().endsWith(".txt") && !r.getId().toLowerCase().endsWith(".md"))
                .collect(groupingBy(
                    Resource::getId,
                    toUnmodifiableSet()
                ));
        }
        return classpathElementsById;
    }

    /**
     * Compile the WAR destinations of all the files in the extension.
     *
     * @return
     */
    public Map<String, FileResource> retrieveFilesByDestination()
    {
        final Map<String, String> fileMappings = configService.getFileMappings();

        if (filesByDestination == null)
        {
            filesByDestination = configService
                .getExtensionResources(FILE)
                .stream()
                .map(r -> (FileResource) r)
                .collect(toUnmodifiableMap(r -> computeDestination(r, fileMappings), identity()));
        }
        return filesByDestination;
    }

    /**
     * Compile the subset of beans defined in an AMP that have an Alfresco
     * class type (i.e. the class is defined in an"org.alfresco..." package).
     *
     * @return
     */
    public Set<BeanResource> retrieveBeansOfAlfrescoTypes()
    {
        if (beansOfAlfrescoTypes == null)
        {
            beansOfAlfrescoTypes = configService
                .getExtensionResources(BEAN)
                .stream()
                .map(r -> (BeanResource) r)
                .filter(r -> r.getBeanClass() != null)
                .filter(r -> r.getBeanClass().startsWith(ORG_ALFRESCO_PREFIX))
                .collect(toUnmodifiableSet());
        }
        return beansOfAlfrescoTypes;
    }

    /**
     * Compute an amp file's destination when applied to a WAR.
     *
     * @param resource
     * @param fileMappings
     * @return
     */
    private static String computeDestination(final FileResource resource, final Map<String, String> fileMappings)
    {
        // Find the most specific/deepest mapping that we can use
        final String matchingSourceMapping = findMostSpecificMapping(fileMappings, resource);

        // We now know the mapping that should apply and we can calculate the destination
        final String destination =
            matchingSourceMapping.isEmpty() ?
            resource.getId() :
            resource.getId().replaceFirst(matchingSourceMapping,
                fileMappings.get(matchingSourceMapping));

        // If the mapping points to 'root' we might have 2 double '/'
        return destination.startsWith("//") ? destination.substring(1) : destination;
    }

    /**
     * Finds the the most specific (deepest in the file tree) mapping that can apply for the give .amp resource
     *
     * @param fileMappings all the mappings
     * @param ampResource  the .amp resource
     * @return the most specific mapping.
     */
    public static String findMostSpecificMapping(final Map<String, String> fileMappings, final FileResource ampResource)
    {
        String matchingSourceMapping = "";
        for (String sourceMapping : fileMappings.keySet())
        {
            if (ampResource.getId().startsWith(sourceMapping + "/") &&
                sourceMapping.length() > matchingSourceMapping.length())
            {
                matchingSourceMapping = sourceMapping;
            }
        }
        return matchingSourceMapping;
    }
}
