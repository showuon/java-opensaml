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

package org.opensaml.saml.saml2.metadata.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.IndexedXMLObjectChildrenList;
import org.opensaml.saml.common.AbstractSignableSAMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * Concrete implementation of {@link org.opensaml.saml.saml2.metadata.EntitiesDescriptor}.
 */
public class EntitiesDescriptorImpl extends AbstractSignableSAMLObject implements EntitiesDescriptor {

    /** Name of this descriptor group. */
    private String name;

    /** ID attribute. */
    private String id;

    /** validUntil attribute. */
    private DateTime validUntil;

    /** cacheDurection attribute. */
    private Long cacheDuration;

    /** Extensions child. */
    private Extensions extensions;

    /** Ordered set of child Entity/Entities Descriptors. */
    private final IndexedXMLObjectChildrenList<SAMLObject> orderedDescriptors;

    /**
     * Constructor.
     * 
     * @param namespaceURI the namespace the element is in
     * @param elementLocalName the local name of the XML element this Object represents
     * @param namespacePrefix the prefix for the given namespace
     */
    protected EntitiesDescriptorImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        orderedDescriptors = new IndexedXMLObjectChildrenList<>(this);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public void setName(String newName) {
        this.name = prepareForAssignment(this.name, newName);
    }

    /** {@inheritDoc} */
    public String getID() {
        return id;
    }

    /** {@inheritDoc} */
    public void setID(String newID) {
        String oldID = this.id;
        this.id = prepareForAssignment(this.id, newID);
        registerOwnID(oldID, this.id);
    }

    /** {@inheritDoc} */
    public boolean isValid() {
        if (null == validUntil) {
            return true;
        }
        
        DateTime now = new DateTime();
        return now.isBefore(validUntil);
    }

    /** {@inheritDoc} */
    public DateTime getValidUntil() {
        return validUntil;
    }

    /** {@inheritDoc} */
    public void setValidUntil(DateTime newValidUntil) {
        validUntil = prepareForAssignment(validUntil, newValidUntil);
    }

    /** {@inheritDoc} */
    public Long getCacheDuration() {
        return cacheDuration;
    }

    /** {@inheritDoc} */
    public void setCacheDuration(Long duration) {
        cacheDuration = prepareForAssignment(cacheDuration, duration);
    }

    /** {@inheritDoc} */
    public Extensions getExtensions() {
        return extensions;
    }

    /** {@inheritDoc} */
    public void setExtensions(Extensions newExtensions) {
        extensions = prepareForAssignment(extensions, newExtensions);
    }

    /** {@inheritDoc} */
    public List<EntitiesDescriptor> getEntitiesDescriptors() {
        return (List<EntitiesDescriptor>) orderedDescriptors.subList(EntitiesDescriptor.ELEMENT_QNAME);
    }

    /** {@inheritDoc} */
    public List<EntityDescriptor> getEntityDescriptors() {
        return (List<EntityDescriptor>) orderedDescriptors.subList(EntityDescriptor.ELEMENT_QNAME);
    }
    
    /** {@inheritDoc} */
    public String getSignatureReferenceID(){
        return id;
    }

    /** {@inheritDoc} */
    public List<XMLObject> getOrderedChildren() {
        ArrayList<XMLObject> children = new ArrayList<>();

        if(getSignature() != null){
            children.add(getSignature());
        }
        
        children.add(getExtensions());
        children.addAll(orderedDescriptors);

        return Collections.unmodifiableList(children);
    }
}