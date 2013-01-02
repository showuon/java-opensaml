/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.core.xml;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.XmlConstants;

/**
 * Interface for element having a <code>@xml:base</code> attribute.
 * 
 */
public interface BaseBearing {

    /** The <code>base</code> attribute local name. */
    public static final String XML_BASE_ATTR_LOCAL_NAME = "base";

    /** The <code>xml:base</code> qualified attribute name. */
    public static final QName XML_BASE_ATTR_NAME =
        new QName(XmlConstants.XML_NS, XML_BASE_ATTR_LOCAL_NAME, XmlConstants.XML_PREFIX);

    /**
     * Returns the <code>@xml:base</code> attribute value.
     * 
     * @return The <code>@xml:base</code> attribute value or <code>null</code>.
     */
    @Nullable public String getXMLBase();

    /**
     * Sets the <code>@xml:base</code> attribute value.
     * 
     * @param newBase The <code>@xml:base</code> attribute value
     */
    public void setXMLBase(@Nullable final String newBase);

}