/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.ampalyser.inventory.model;

/**
 * @author Lucian Tuca
 * created on 07/05/2020
 */
public interface Resource
{
    enum Type
    {
        FILE, BEAN, ALFRESCO_PUBLIC_API, CLASSPATH_ELEMENT
    }

    Type getType();

    boolean isFile();

    boolean isBean();

    boolean isPublicApi();

    boolean isClasspathElement();

    String getDefiningObject();

    void setDefiningObject(String definingObject);

}
