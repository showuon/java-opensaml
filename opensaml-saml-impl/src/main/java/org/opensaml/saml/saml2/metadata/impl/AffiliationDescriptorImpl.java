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

/**
 * 
 */

package org.opensaml.saml.saml2.metadata.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.AttributeMap;
import org.opensaml.core.xml.util.XMLObjectChildrenList;
import org.opensaml.saml.common.AbstractSignableSAMLObject;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.AffiliateMember;
import org.opensaml.saml.saml2.metadata.AffiliationDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;

/**
 * Concrete implementation of {@link org.opensaml.saml.saml2.metadata.AffiliationDescriptor}.
 */
public class AffiliationDescriptorImpl extends AbstractSignableSAMLObject implements AffiliationDescriptor {

    /** ID of the owner of this affiliation. */
    private String ownerID;
    
    /** ID attribute. */
    private String id;

    /** validUntil attribute. */
    private DateTime validUntil;

    /** cacheDurection attribute. */
    private Long cacheDuration;

    /** Extensions child. */
    private Extensions extensions;
    
    /** "anyAttribute" attributes. */
    private final AttributeMap unknownAttributes;

    /** Members of this affiliation. */
    private final XMLObjectChildrenList<AffiliateMember> members;

    /** Key descriptors for this role. */
    private final XMLObjectChildrenList<KeyDescriptor> keyDescriptors;

    /**
     * Constructor.
     * 
     * @param namespaceURI namespace
     * @param elementLocalName localname
     * @param namespacePrefix prefix
     */
    protected AffiliationDescriptorImpl(String namespaceURI, String elementLocalName, String namespacePrefix) {
        super(namespaceURI, elementLocalName, namespacePrefix);
        unknownAttributes = new AttributeMap(this);
        members = new XMLObjectChildrenList<>(this);
        keyDescriptors = new XMLObjectChildrenList<>(this);
    }

    /** {@inheritDoc} */
    @Override
    public String getOwnerID() {
        return ownerID;
    }

    /** {@inheritDoc} */
    @Override
    public void setOwnerID(String newOwnerID) {
        if (newOwnerID != null && newOwnerID.length() > 1024) {
            throw new IllegalArgumentException("Owner ID can not exceed 1024 characters in length");
        }
        ownerID = prepareForAssignment(ownerID, newOwnerID);
    }
    
    /** {@inheritDoc} */
    @Override
    public String getID() {
        return id;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setID(String newID) {
        String oldID = this.id;
        this.id = prepareForAssignment(this.id, newID);
        registerOwnID(oldID, this.id);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        if (null == validUntil) {
            return true;
        }

        DateTime now = new DateTime();
        return now.isBefore(validUntil);
    }

    /** {@inheritDoc} */
    @Override
    public DateTime getValidUntil() {
        return validUntil;
    }

    /** {@inheritDoc} */
    @Override
    public void setValidUntil(DateTime theValidUntil) {
        this.validUntil = prepareForAssignment(this.validUntil, theValidUntil);
    }

    /** {@inheritDoc} */
    @Override
    public Long getCacheDuration() {
        return cacheDuration;
    }

    /** {@inheritDoc} */
    @Override
    public void setCacheDuration(Long duration) {
        cacheDuration = prepareForAssignment(cacheDuration, duration);
    }

    /** {@inheritDoc} */
    @Override
    public Extensions getExtensions() {
        return extensions;
    }

    /** {@inheritDoc} */
    @Override
    public void setExtensions(Extensions theExtensions) {
        this.extensions = prepareForAssignment(this.extensions, theExtensions);
    }

    /** {@inheritDoc} */
    @Override
    public List<AffiliateMember> getMembers() {
        return members;
    }

    /** {@inheritDoc} */
    @Override
    public List<KeyDescriptor> getKeyDescriptors() {
        return keyDescriptors;
    }
    
    /** {@inheritDoc} */
    @Override
    public AttributeMap getUnknownAttributes() {
        return unknownAttributes;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getSignatureReferenceID(){
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public List<XMLObject> getOrderedChildren() {
        ArrayList<XMLObject> children = new ArrayList<>();

        if(getSignature() != null){
            children.add(getSignature());
        }
        
        children.add(getExtensions());

        children.addAll(getMembers());

        children.addAll(getKeyDescriptors());

        return Collections.unmodifiableList(children);
    }

}