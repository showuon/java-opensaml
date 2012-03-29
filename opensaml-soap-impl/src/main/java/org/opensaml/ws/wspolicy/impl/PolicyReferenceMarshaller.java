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

package org.opensaml.ws.wspolicy.impl;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.soap.wspolicy.PolicyReference;
import org.w3c.dom.Element;

/**
 * Marshaller for the wsp:PolicyReference element.
 * 
 */
public class PolicyReferenceMarshaller extends AbstractWSPolicyObjectMarshaller {

    /** {@inheritDoc} */
    protected void marshallAttributes(XMLObject xmlObject, Element domElement) throws MarshallingException {
        PolicyReference pr = (PolicyReference) xmlObject;
        
        if (pr.getURI() != null) {
            domElement.setAttributeNS(null, PolicyReference.URI_ATTRIB_NAME, pr.getURI());
        }
        
        if (pr.getDigest() != null) {
            domElement.setAttributeNS(null, PolicyReference.DIGEST_ATTRIB_NAME, pr.getDigest());
        }
        
        if (pr.getDigestAlgorithm() != null) {
            domElement.setAttributeNS(null, PolicyReference.DIGEST_ALGORITHM_ATTRIB_NAME, pr.getDigestAlgorithm());
        }
        
        XMLObjectSupport.marshallAttributeMap(pr.getUnknownAttributes(), domElement);
        
    }
}
