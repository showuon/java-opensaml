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

package org.opensaml.saml.saml1.binding.artifact;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SamlArtifactContext;
import org.opensaml.saml.saml1.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder of SAML 1, type 0x001, artifacts.
 */
public class SAML1ArtifactType0001Builder implements SAML1ArtifactBuilder<SAML1ArtifactType0001> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML1ArtifactType0001Builder.class);

    /** {@inheritDoc} */
    public SAML1ArtifactType0001 buildArtifact(byte[] artifact) {
        return SAML1ArtifactType0001.parseArtifact(artifact);
    }

    /** {@inheritDoc} */
    public SAML1ArtifactType0001 buildArtifact(MessageContext<SAMLObject> requestContext, Assertion assertion) {
        try {
            MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
            byte[] source = sha1Digester.digest(getSourceEntityId(requestContext).getBytes());

            SecureRandom handleGenerator = SecureRandom.getInstance("SHA1PRNG");
            byte[] assertionHandle = new byte[20];
            handleGenerator.nextBytes(assertionHandle);

            return new SAML1ArtifactType0001(source, assertionHandle);
        } catch (NoSuchAlgorithmException e) {
            log.error("JVM does not support required cryptography algorithms.", e);
            throw new InternalError("JVM does not support required cryptography algorithms: SHA-1 and/or SHA1PRNG.");
        }
    }
    
    /**
     * Get the artifact context.
     * 
     * @param requestContext the current message context
     * @return the SAML artifact context, or null
     */
    protected SamlArtifactContext getArtifactContext(MessageContext<SAMLObject> requestContext) {
        return requestContext.getSubcontext(SamlArtifactContext.class, false);
    }

    /**
     * Get the local entityId.
     * 
     * @param requestContext the message context
     * 
     * @return the local entityId
     */
    protected String getSourceEntityId(MessageContext<SAMLObject> requestContext) {
        SamlArtifactContext artifactContext = getArtifactContext(requestContext);
        Constraint.isNotNull(artifactContext, "Message context did not contain a SamlArtifactContext");
        Constraint.isNotNull(artifactContext.getSourceEntityId(), 
                "SamlArtifactContext did not contain a source entityID");
        return artifactContext.getSourceEntityId();
    }
    
}