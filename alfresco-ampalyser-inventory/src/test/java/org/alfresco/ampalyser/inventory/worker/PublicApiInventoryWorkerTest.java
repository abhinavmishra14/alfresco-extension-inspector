/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser.inventory.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

import org.alfresco.ampalyser.inventory.EntryProcessor;
import org.alfresco.ampalyser.inventory.data.classes.ClassDeprecated;
import org.alfresco.ampalyser.inventory.data.classes.ClassWithAlfrescoApiAnnotation;
import org.alfresco.ampalyser.inventory.data.classes.ClassWithAlfrescoApiAnnotationDeprecated;
import org.alfresco.ampalyser.model.AlfrescoPublicApiResource;
import org.alfresco.ampalyser.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublicApiInventoryWorkerTest
{
    private static final String TEST_ALFRESCO_PUBLIC_API = "Lorg/alfresco/ampalyser/inventory/data/classes/TestAlfrescoPublicApi;";
    @Spy
    private AlfrescoPublicApiInventoryWorker worker = new AlfrescoPublicApiInventoryWorker(new EntryProcessor());

    @Test
    public void testWorkerHasProperType()
    {
        assertSame(Resource.Type.ALFRESCO_PUBLIC_API, worker.getType());
    }

    @Test
    public void testCanProcessClassEntry()
    {
        ZipEntry entry = new ZipEntry("AClass.class");
        assertFalse(worker.canProcessEntry(entry,"source"));

        entry = new ZipEntry("org/notalfresco/AClass.class");
        assertFalse(worker.canProcessEntry(entry,"source"));

        entry = new ZipEntry("org/alfresco/apackage/AClass.class");
        assertTrue(worker.canProcessEntry(entry,"source"));
    }

    @Test
    public void testCantProcessNonClassEntry()
    {
        ZipEntry entry = new ZipEntry("afile.xml");
        assertFalse(worker.canProcessEntry(entry,"source"));

        entry = new ZipEntry("org/alfresco/afile.xml");
        assertFalse(worker.canProcessEntry(entry,"source"));
    }

    @Test
    public void testClassWithAlfrescoPublicApiAnnotation() throws Exception
    {
        Class testClass = ClassWithAlfrescoApiAnnotation.class;

        ZipEntry zipEntry = new ZipEntry(getClassRelativePath(testClass));
        byte[] data = getClassData(testClass);

        doReturn(TEST_ALFRESCO_PUBLIC_API).when(worker).getPublicAnnotationType();
        List<Resource> resources = worker.processZipEntry(zipEntry, data, "source");
        assertEquals(1, resources.size());

        assertTrue(resources.get(0) instanceof AlfrescoPublicApiResource);

        AlfrescoPublicApiResource resource = (AlfrescoPublicApiResource) resources.get(0);
        assertEquals(Resource.Type.ALFRESCO_PUBLIC_API, resource.getType());
        assertEquals(testClass.getName(), resource.getId(), testClass.getName() + " is part of AlfrescoPublicApi");
        assertFalse(resource.isDeprecated());
    }

    @Test
    public void testClassHasAlfrescoPublicApiAnnotationAndDeprecated() throws Exception
    {
        Class testClass = ClassWithAlfrescoApiAnnotationDeprecated.class;

        ZipEntry zipEntry = new ZipEntry(getClassRelativePath(testClass));
        byte[] data = getClassData(testClass);

        doReturn(TEST_ALFRESCO_PUBLIC_API).when(worker).getPublicAnnotationType();
        List<Resource> resources = worker.processZipEntry(zipEntry, data, "source");
        assertEquals(1, resources.size());

        assertTrue(resources.get(0) instanceof AlfrescoPublicApiResource);

        AlfrescoPublicApiResource resource = (AlfrescoPublicApiResource) resources.get(0);
        assertEquals(testClass.getName(), resource.getId(), testClass.getName() + " is part of AlfrescoPublicApi");
        assertTrue(resource.isDeprecated());
    }

    @Test
    public void testClassNoAlfrescoPublicApiAnnotationAndDeprecated() throws Exception
    {
        Class testClass = ClassDeprecated.class;

        ZipEntry zipEntry = new ZipEntry(getClassRelativePath(testClass));
        byte[] data = getClassData(testClass);

        doReturn(TEST_ALFRESCO_PUBLIC_API).when(worker).getPublicAnnotationType();
        List<Resource> resources = worker.processZipEntry(zipEntry, data, "source");
        assertEquals(0, resources.size());
    }

    private byte[] getClassData(Class clazz) throws IOException
    {
        String name = clazz.getName();
        int i = name.lastIndexOf('.');
        if (i > 0)
        {
            name = name.substring(i + 1);
        }
        InputStream clsStream = clazz.getResourceAsStream(name + ".class");
        return  clsStream.readAllBytes();
    }

    private String getClassRelativePath(Class clazz)
    {
        String path = clazz.getName().replaceAll("\\.", "/") + ".class";
        return path;
    }
}
