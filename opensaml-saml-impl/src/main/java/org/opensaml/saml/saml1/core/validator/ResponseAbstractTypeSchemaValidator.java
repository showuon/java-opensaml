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

package org.opensaml.saml.saml1.core.validator;

import org.opensaml.core.xml.validation.ValidationException;
import org.opensaml.core.xml.validation.Validator;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.ResponseAbstractType;

import com.google.common.base.Strings;

/**
 * Checks {@link org.opensaml.saml.saml1.core.ResponseAbstractType} for Schema compliance.
 */
public class ResponseAbstractTypeSchemaValidator<ResponseType extends ResponseAbstractType> implements Validator<ResponseType> {

    /** {@inheritDoc} */
    public void validate(ResponseType response) throws ValidationException {
        validateVersion(response);

        validateID(response);

        validateIssueInstant(response);
    }

    /**
     * Validates that this is SAML1.0 or SAML 1.1
     * 
     * @param response
     * @throws ValidationException
     */
    protected void validateVersion(ResponseAbstractType response) throws ValidationException {
        if (response.getVersion() != SAMLVersion.VERSION_10 || response.getVersion() != SAMLVersion.VERSION_11) {
            throw new ValidationException("Invalid Version");
        }
    }

    /**
     * Validate that the ID is present and valid
     * 
     * @param response
     * @throws ValidationException
     */
    protected void validateID(ResponseAbstractType response) throws ValidationException {
        if (Strings.isNullOrEmpty(response.getID())) {
            throw new ValidationException("RequestID is null, empty or whitespace");
        }
    }

    /**
     * Validate that the IssueInstant is present.
     * 
     * @param response
     * @throws ValidationException
     */
    protected void validateIssueInstant(ResponseAbstractType response) throws ValidationException {
        if (response.getIssueInstant() == null) {
            throw new ValidationException("No IssueInstant attribute present");
        }
    }

}