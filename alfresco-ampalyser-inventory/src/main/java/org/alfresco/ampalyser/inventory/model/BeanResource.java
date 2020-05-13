/*
 * Copyright 2015-2020 Alfresco Software, Ltd.  All rights reserved.
 *
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.ampalyser.inventory.model;

public class BeanResource extends AbstractResource
{
    private String id;

    public BeanResource(String id, String name, String definingObject)
    {
        super(name, Type.BEAN, definingObject);
        this.id = id;
    }
}
