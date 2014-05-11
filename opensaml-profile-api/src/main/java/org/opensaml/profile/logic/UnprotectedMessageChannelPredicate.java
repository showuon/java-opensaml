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

package org.opensaml.profile.logic;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.MessageChannelSecurityContext;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Predicate;

/**
 * A predicate that returns true iff the outbound message context does not contain a
 * {@link MessageChannelSecurityContext} such that {@link MessageChannelSecurityContext#isIntegrityActive()}
 * is true.
 */
public class UnprotectedMessageChannelPredicate implements Predicate<ProfileRequestContext> {

    /** {@inheritDoc} */
    public boolean apply(@Nullable final ProfileRequestContext input) {
        return input == null || input.getOutboundMessageContext() == null
                || !input.getOutboundMessageContext().getSubcontext(
                        MessageChannelSecurityContext.class, true).isIntegrityActive();
    }

}