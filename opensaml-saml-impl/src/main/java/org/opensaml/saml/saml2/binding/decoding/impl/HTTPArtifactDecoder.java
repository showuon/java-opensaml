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

package org.opensaml.saml.saml2.binding.decoding.impl;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * SAML 2 Artifact Binding decoder, support both HTTP GET and POST.
 * 
 * <strong>NOTE: This decoder is not yet implemented.</strong>
 * */
public class HTTPArtifactDecoder extends BaseHttpServletRequestXMLMessageDecoder<SAMLObject> 
        implements SAMLMessageDecoder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPArtifactDecoder.class);

    /** Optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created. */
    @Nullable private BindingDescriptor bindingDescriptor;

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String getBindingURI() {
        return SAMLConstants.SAML2_ARTIFACT_BINDING_URI;
    }

    /**
     * Get an optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created.
     * 
     * @return binding descriptor
     */
    @Nullable public BindingDescriptor getBindingDescriptor() {
        return bindingDescriptor;
    }
    
    /**
     * Set an optional {@link BindingDescriptor} to inject into {@link SAMLBindingContext} created.
     * 
     * @param descriptor a binding descriptor
     */
    public void setBindingDescriptor(@Nullable final BindingDescriptor descriptor) {
        bindingDescriptor = descriptor;
    }
    
    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        MessageContext<SAMLObject> messageContext = new MessageContext<>();
        HttpServletRequest request = getHttpServletRequest();

        String relayState = StringSupport.trim(request.getParameter("RelayState"));
        log.debug("Decoded SAML relay state of: {}", relayState);
        SAMLBindingSupport.setRelayState(messageContext, relayState);
        
        processArtifact(messageContext, request);

        populateBindingContext(messageContext);
        
        setMessageContext(messageContext);
    }
    
    /**
     * Process the incoming artifact by decoding the artifacts, dereferencing it from the artifact issuer and 
     * storing the resulting protocol message in the message context.
     * 
     * @param messageContext the message context being processed
     * @param request the HTTP servlet request
     * 
     * @throws MessageDecodingException thrown if there is a problem decoding or dereferencing the artifact
     */
    protected void processArtifact(MessageContext messageContext, HttpServletRequest request) 
            throws MessageDecodingException {
        String encodedArtifact = StringSupport.trimOrNull(request.getParameter("SAMLart"));
        if (encodedArtifact == null) {
            log.error("URL SAMLart parameter was missing or did not contain a value.");
            throw new MessageDecodingException("URL TARGET parameter was missing or did not contain a value.");
        }
        
        // TODO decode artifact; resolve issuer resolution endpoint; dereference using ArtifactResolve
        // over synchronous backchannel binding; store resultant protocol message as the inbound SAML message.
    }
    
    /**
     * Populate the context which carries information specific to this binding.
     * 
     * @param messageContext the current message context
     */
    protected void populateBindingContext(MessageContext<SAMLObject> messageContext) {
        SAMLBindingContext bindingContext = messageContext.getSubcontext(SAMLBindingContext.class, true);
        bindingContext.setBindingUri(getBindingURI());
        bindingContext.setBindingDescriptor(bindingDescriptor);
        bindingContext.setHasBindingSignature(false);
        bindingContext.setIntendedDestinationEndpointURIRequired(false);
    }

}