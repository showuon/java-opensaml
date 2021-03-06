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

package org.opensaml.saml.metadata.resolver.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.metadata.resolver.DynamicMetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.saml2.common.SAML2Support;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Abstract subclass for metadata resolvers that resolve metadata dynamically, as needed and on demand.
 */
public abstract class AbstractDynamicMetadataResolver extends AbstractMetadataResolver 
        implements DynamicMetadataResolver {
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractDynamicMetadataResolver.class);
    
    /** Timer used to schedule background metadata update tasks. */
    private Timer taskTimer;
    
    /** Whether we created our own task timer during object construction. */
    private boolean createdOwnTaskTimer;
    
    /** Minimum cache duration. */
    @Duration @Positive private Long minCacheDuration;
    
    /** Maximum cache duration. */
    @Duration @Positive private Long maxCacheDuration;
    
    /** Factor used to compute when the next refresh interval will occur. Default value: 0.75 */
    @Positive private Float refreshDelayFactor;
    
    /** The maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed. */
    @Duration @Positive private Long maxIdleEntityData;
    
    /** Flag indicating whether idle entity data should be removed. */
    private boolean removeIdleEntityData;
    
    /** The interval in milliseconds at which the cleanup task should run. */
    @Duration @Positive private Long cleanupTaskInterval;
    
    /** The backing store cleanup sweeper background task. */
    private BackingStoreCleanupSweeper cleanupTask;
    
    /**
     * Constructor.
     *
     * @param backgroundTaskTimer the {@link Timer} instance used to run resolver background managment tasks
     */
    public AbstractDynamicMetadataResolver(@Nullable final Timer backgroundTaskTimer) {
        super();
        
        if (backgroundTaskTimer == null) {
            taskTimer = new Timer(true);
            createdOwnTaskTimer = true;
        } else {
            taskTimer = backgroundTaskTimer;
        }
        
        // Default to 10 minutes.
        minCacheDuration = 10*60*1000L;
        
        // Default to 8 hours.
        maxCacheDuration = 8*60*60*1000L;
        
        refreshDelayFactor = 0.75f;
        
        // Default to 30 minutes.
        cleanupTaskInterval = 30*60*1000L;
        
        // Default to 8 hours.
        maxIdleEntityData = 8*60*60*1000L;
        
        // Default to removing idle metadata
        removeIdleEntityData = true;
    }
    
    /**
     *  Get the minimum cache duration for metadata.
     *  
     *  <p>Defaults to: 10 minutes.</p>
     *  
     * @return the minimum cache duration, in milliseconds
     */
    @Nonnull public Long getMinCacheDuration() {
        return minCacheDuration;
    }

    /**
     *  Set the minimum cache duration for metadata.
     *  
     *  <p>Defaults to: 10 minutes.</p>
     *  
     * @param duration the minimum cache duration, in milliseconds
     */
    public void setMinCacheDuration(@Nonnull final Long duration) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        minCacheDuration = Constraint.isNotNull(duration, "Minimum cache duration may not be null");
    }

    /**
     *  Get the maximum cache duration for metadata.
     *  
     *  <p>Defaults to: 8 hours.</p>
     *  
     * @return the maximum cache duration, in milliseconds
     */
    @Nonnull public Long getMaxCacheDuration() {
        return maxCacheDuration;
    }

    /**
     *  Set the maximum cache duration for metadata.
     *  
     *  <p>Defaults to: 8 hours.</p>
     *  
     * @param duration the maximum cache duration, in milliseconds
     */
    public void setMaxCacheDuration(@Nonnull final Long duration) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        maxCacheDuration = Constraint.isNotNull(duration, "Maximum cache duration may not be null");
    }
    
    /**
     * Gets the delay factor used to compute the next refresh time.
     * 
     * <p>Defaults to:  0.75.</p>
     * 
     * @return delay factor used to compute the next refresh time
     */
    public Float getRefreshDelayFactor() {
        return refreshDelayFactor;
    }

    /**
     * Sets the delay factor used to compute the next refresh time. The delay must be between 0.0 and 1.0, exclusive.
     * 
     * <p>Defaults to:  0.75.</p>
     * 
     * @param factor delay factor used to compute the next refresh time
     */
    public void setRefreshDelayFactor(Float factor) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (factor <= 0 || factor >= 1) {
            throw new IllegalArgumentException("Refresh delay factor must be a number between 0.0 and 1.0, exclusive");
        }

        refreshDelayFactor = factor;
    }

    /**
     * Get the flag indicating whether idle entity data should be removed. 
     * 
     * @return true if idle entity data should be removed, false otherwise
     */
    public boolean isRemoveIdleEntityData() {
        return removeIdleEntityData;
    }

    /**
     * Set the flag indicating whether idle entity data should be removed. 
     * 
     * @param flag true if idle entity data should be removed, false otherwise
     */
    public void setRemoveIdleEntityData(boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        removeIdleEntityData = flag;
    }

    /**
     * Get the maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed.
     * 
     * <p>Defaults to: 8 hours.</p>
     * 
     * @return return the maximum idle time in milliseconds
     */
    @Nonnull public Long getMaxIdleEntityData() {
        return maxIdleEntityData;
    }

    /**
     * Set the maximum idle time in milliseconds for which the resolver will keep data for a given entityID, 
     * before it is removed.
     * 
     * <p>Defaults to: 8 hours.</p>
     * 
     * @param max the maximum entity data idle time, in milliseconds
     */
    public void setMaxIdleEntityData(@Nonnull final Long max) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        maxIdleEntityData = Constraint.isNotNull(max, "Max idle entity data may not be null");
    }

    /**
     * Get the interval in milliseconds at which the cleanup task should run.
     * 
     * <p>Defaults to: 30 minutes.</p>
     * 
     * @return return the interval, in milliseconds
     */
    @Nonnull public Long getCleanupTaskInterval() {
        return cleanupTaskInterval;
    }

    /**
     * Set the interval in milliseconds at which the cleanup task should run.
     * 
     * <p>Defaults to: 30 minutes.</p>
     * 
     * @param interval the interval to set, in milliseconds
     */
    public void setCleanupTaskInterval(@Nonnull final Long interval) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        cleanupTaskInterval = Constraint.isNotNull(interval, "Cleanup task interval may not be null");
    }



    /** {@inheritDoc} */
    @Nonnull public Iterable<EntityDescriptor> resolve(@Nonnull final CriteriaSet criteria) throws ResolverException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        
        EntityIdCriterion entityIdCriterion = criteria.get(EntityIdCriterion.class);
        if (entityIdCriterion == null || Strings.isNullOrEmpty(entityIdCriterion.getEntityId())) {
            //TODO throw or just log?
            throw new ResolverException("Entity Id was not supplied in criteria set");
        }
        
        String entityID = StringSupport.trimOrNull(criteria.get(EntityIdCriterion.class).getEntityId());
        log.debug("Attempting to resolve metadata for entityID: {}", entityID);
        
        EntityManagementData mgmtData = getBackingStore().getManagementData(entityID);
        Lock readLock = mgmtData.getReadWriteLock().readLock();
        try {
            readLock.lock();
            
            if (!shouldAttemptRefresh(mgmtData)) {
                List<EntityDescriptor> descriptors = lookupEntityID(entityID);
                if (!descriptors.isEmpty()) {
                    log.debug("Found requested metadata in backing store, returning");
                    return descriptors;
                } else {
                    log.debug("Did not find requested metadata in backing store, will attempt to resolve dynamically");
                }
        
            } else {
                log.debug("Metadata was indicated to be refreshed based on refresh trigger time");
            }
        } finally {
            readLock.unlock();
        }
        
        return resolveFromOriginSource(criteria);
    }
    
    /**
     * Fetch metadata from an origin source based on the input criteria, store it in the backing store 
     * and then return it.
     * 
     * @param criteria the input criteria set
     * @return the resolved metadata
     * @throws ResolverException  if there is a fatal error attempting to resolve the metadata
     */
    @Nonnull @NonnullElements protected Iterable<EntityDescriptor> resolveFromOriginSource(
            @Nonnull final CriteriaSet criteria) throws ResolverException {
        
        String entityID = StringSupport.trimOrNull(criteria.get(EntityIdCriterion.class).getEntityId());
        EntityManagementData mgmtData = getBackingStore().getManagementData(entityID);
        Lock writeLock = mgmtData.getReadWriteLock().writeLock(); 
        
        try {
            writeLock.lock();
            
            // It's possible that multiple threads fall into here and attempt to preemptively refresh. 
            // This check should ensure that only 1 actually successfully does it, b/c the refresh
            // trigger time will be updated as seen by the subsequent ones. 
            if (!shouldAttemptRefresh(mgmtData)) {
                List<EntityDescriptor> descriptors = lookupEntityID(entityID);
                if (!descriptors.isEmpty()) {
                    log.debug("Metadata was resolved and stored by another thread " 
                            + "while this thread was waiting on the write lock");
                    return descriptors;
                }
            }
            
            log.debug("Resolving metadata dynamically for entity ID: {}", entityID);
            
            XMLObject root = fetchFromOriginSource(criteria);
            if (root == null) {
                log.debug("No metadata was fetched from the origin source");
            } else {
                try {
                    processNewMetadata(root, entityID);
                } catch (FilterException e) {
                    log.error("Metadata filtering problem processing new metadata", e);
                }
            }
            
            return lookupEntityID(entityID);
            
        } catch (IOException e) {
            log.error("Error fetching metadata from origin source", e);
            return lookupEntityID(entityID);
        } finally {
            writeLock.unlock();
        }
        
    }

    /**
     * Fetch the metadata from the origin source.
     * 
     * @param criteria the input criteria set
     * @return the resolved metadata root XMLObject, or null if metadata could not be fetched
     * @throws IOException if there is a fatal error fetching metadata from the origin source
     */
    @Nullable protected abstract XMLObject fetchFromOriginSource(@Nonnull final CriteriaSet criteria) 
            throws IOException;

    /** {@inheritDoc} */
    @Nonnull @NonnullElements protected List<EntityDescriptor> lookupEntityID(@Nonnull String entityID) 
            throws ResolverException {
        getBackingStore().getManagementData(entityID).recordEntityAccess();
        return super.lookupEntityID(entityID);
    }

    /**
     * Process the specified new metadata document, including metadata filtering, and store the 
     * processed metadata in the backing store.
     * 
     * <p>
     * In order to be processed successfully, the metadata (after filtering) must be an instance of
     * {@link EntityDescriptor} and its <code>entityID</code> value must match the value supplied
     * as the required <code>expectedEntityID</code> argument.
     * </p>
     * 
     * @param root the root of the new metadata document being processed
     * @param expectedEntityID the expected entityID of the resolved metadata
     * 
     * @throws FilterException if there is a problem filtering the metadata
     */
    @Nonnull protected void processNewMetadata(@Nonnull final XMLObject root, @Nonnull final String expectedEntityID) 
            throws FilterException {
        
        XMLObject filteredMetadata = filterMetadata(root);
        
        if (filteredMetadata == null) {
            log.info("Metadata filtering process produced a null document, resulting in an empty data set");
            return;
        }
        
        if (filteredMetadata instanceof EntityDescriptor) {
            EntityDescriptor entityDescriptor = (EntityDescriptor) filteredMetadata;
            if (!Objects.equals(entityDescriptor.getEntityID(), expectedEntityID)) {
                log.warn("New metadata's entityID '{}' does not match expected entityID '{}', will not process", 
                        entityDescriptor.getEntityID(), expectedEntityID);
               return; 
            }
            preProcessEntityDescriptor(entityDescriptor, getBackingStore());
        } else {
            log.warn("Document root was not an EntityDescriptor: {}", root.getClass().getName());
        }
    
    }
    
    /** {@inheritDoc} */
    protected void preProcessEntityDescriptor(@Nonnull EntityDescriptor entityDescriptor, 
            @Nonnull EntityBackingStore backingStore) {
        
        String entityID = StringSupport.trimOrNull(entityDescriptor.getEntityID());
        
        removeByEntityID(entityID, backingStore);
        
        super.preProcessEntityDescriptor(entityDescriptor, backingStore);
        
        DynamicEntityBackingStore dynamicBackingStore = (DynamicEntityBackingStore) backingStore;
        EntityManagementData mgmtData = dynamicBackingStore.getManagementData(entityID);
        
        DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        log.debug("For metadata expiration and refresh computation, 'now' is : {}", now);
        
        mgmtData.setLastUpdateTime(now);
        
        mgmtData.setExpirationTime(computeExpirationTime(entityDescriptor, now));
        log.debug("Computed metadata expiration time: {}", mgmtData.getExpirationTime());
        
        mgmtData.setRefreshTriggerTime(computeRefreshTriggerTime(mgmtData.getExpirationTime(), now));
        log.debug("Computed refresh trigger time: {}", mgmtData.getRefreshTriggerTime());
    }

    /**
     * Compute the effective expiration time for the specified metadata.
     * 
     * @param entityDescriptor the EntityDescriptor instance to evaluate
     * @param now the current date time instant
     * @return the effective expiration time for the metadata
     */
    @Nonnull protected DateTime computeExpirationTime(@Nonnull final EntityDescriptor entityDescriptor,
            @Nonnull final DateTime now) {
        
        DateTime lowerBound = now.toDateTime(ISOChronology.getInstanceUTC()).plus(getMinCacheDuration());
        
        DateTime expiration = SAML2Support.getEarliestExpiration(entityDescriptor, 
                now.plus(getMaxCacheDuration()), now);
        if (expiration.isBefore(lowerBound)) {
            expiration = lowerBound;
        }
        
        return expiration;
    }
    
    /**
     * Compute the refresh trigger time.
     * 
     * @param expirationTime the time at which the metadata effectively expires
     * @param nowDateTime the current date time instant
     * 
     * @return the time after which refresh attempt(s) should be made
     */
    @Nonnull protected DateTime computeRefreshTriggerTime(@Nullable final DateTime expirationTime,
            @Nonnull final DateTime nowDateTime) {
        
        DateTime nowDateTimeUTC = nowDateTime.toDateTime(ISOChronology.getInstanceUTC());
        long now = nowDateTimeUTC.getMillis();

        long expireInstant = 0;
        if (expirationTime != null) {
            expireInstant = expirationTime.toDateTime(ISOChronology.getInstanceUTC()).getMillis();
        }
        long refreshDelay = (long) ((expireInstant - now) * getRefreshDelayFactor());

        // if the expiration time was null or the calculated refresh delay was less than the floor
        // use the floor
        if (refreshDelay < getMinCacheDuration()) {
            refreshDelay = getMinCacheDuration();
        }

        return nowDateTimeUTC.plus(refreshDelay);
    }
    
    /**
     * Determine whether should attempt to refresh the metadata, based on stored refresh trigger time.
     * 
     * @param mgmtData the entity'd management data
     * @return true if should attempt refresh, false otherwise
     */
    protected boolean shouldAttemptRefresh(@Nonnull final EntityManagementData mgmtData) {
        DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        return now.isAfter(mgmtData.getRefreshTriggerTime());
        
    }

    /** {@inheritDoc} */
    @Nonnull protected DynamicEntityBackingStore createNewBackingStore() {
        return new DynamicEntityBackingStore();
    }
    
    /** {@inheritDoc} */
    @NonnullAfterInit protected DynamicEntityBackingStore getBackingStore() {
        return (DynamicEntityBackingStore) super.getBackingStore();
    }
    
    /** {@inheritDoc} */
    protected void initMetadataResolver() throws ComponentInitializationException {
        super.initMetadataResolver();
        setBackingStore(createNewBackingStore());
        
        cleanupTask = new BackingStoreCleanupSweeper();
        // Start with a delay of 1 minute, run at the user-specified interval
        taskTimer.schedule(cleanupTask, 1*60*1000, getCleanupTaskInterval());
    }
    
   /** {@inheritDoc} */
    protected void doDestroy() {
        cleanupTask.cancel();
        if (createdOwnTaskTimer) {
            taskTimer.cancel();
        }
        cleanupTask = null;
        taskTimer = null;
        
        super.doDestroy();
    }
    
    /**
     * Specialized entity backing store implementation for dynamic metadata resolvers.
     */
    protected class DynamicEntityBackingStore extends EntityBackingStore {
        
        /** Map holding management data for each entityID. */
        private Map<String, EntityManagementData> mgmtDataMap;
        
        /** Constructor. */
        protected DynamicEntityBackingStore() {
            super();
            mgmtDataMap = new ConcurrentHashMap<>();
        }
        
        /**
         * Get the management data for the specified entityID.
         * 
         * @param entityID the input entityID
         * @return the corresponding management data
         */
        @Nonnull public EntityManagementData getManagementData(@Nonnull final String entityID) {
            Constraint.isNotNull(entityID, "EntityID may not be null");
            EntityManagementData entityData = mgmtDataMap.get(entityID);
            if (entityData != null) {
                return entityData;
            }
            
            // TODO use intern-ed String here for monitor target?
            synchronized (this) {
                // Check again in case another thread beat us into the monitor
                entityData = mgmtDataMap.get(entityID);
                if (entityData != null) {
                    return entityData;
                } else {
                    entityData = new EntityManagementData(entityID);
                    mgmtDataMap.put(entityID, entityData);
                    return entityData;
                }
            }
        }
        
        /**
         * Remove the management data for the specified entityID.
         * 
         * @param entityID the input entityID
         */
        public void removeManagementData(@Nonnull final String entityID) {
            Constraint.isNotNull(entityID, "EntityID may not be null");
            // TODO use intern-ed String here for monitor target?
            synchronized (this) {
                mgmtDataMap.remove(entityID);
            }
        }
        
    }
    
    /**
     * Class holding per-entity management data.
     */
    protected class EntityManagementData {
        
        /** The entity ID managed by this instance. */
        private String entityID;
        
        /** Last update time of the associated metadata. */
        private DateTime lastUpdateTime;
        
        /** Expiration time of the associated metadata. */
        private DateTime expirationTime;
        
        /** Time at which should start attempting to refresh the metadata. */
        private DateTime refreshTriggerTime;
        
        /** The last time in milliseconds at which the entity's backing store data was accessed. */
        private DateTime lastAccessedTime;
        
        /** Read-write lock instance which governs access to the entity's backing store data. */
        private ReadWriteLock readWriteLock;
        
        /** Constructor. 
         * 
         * @param id the entity ID managed by this instance
         */
        protected EntityManagementData(@Nonnull final String id) {
            entityID = Constraint.isNotNull(id, "Entity ID was null");
            expirationTime = new DateTime(ISOChronology.getInstanceUTC()).plus(getMaxCacheDuration());
            refreshTriggerTime = new DateTime(ISOChronology.getInstanceUTC()).plus(getMaxCacheDuration());
            lastAccessedTime = new DateTime(ISOChronology.getInstanceUTC());
            readWriteLock = new ReentrantReadWriteLock(true);
        }
        
        /**
         * Get the entity ID managed by this instance.
         * 
         * @return the entity ID
         */
        @Nonnull public String getEntityID() {
            return entityID;
        }
        
        /**
         * Get the last update time of the metadata. 
         * 
         * @return the last update time, or null if no metadata is yet loaded for the entity
         */
        @Nullable public DateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        /**
         * Set the last update time of the metadata.
         * 
         * @param dateTime the last update time
         */
        public void setLastUpdateTime(@Nonnull final DateTime dateTime) {
            lastUpdateTime = dateTime;
        }
        
        /**
         * Get the expiration time of the metadata. 
         * 
         * @return the expiration time
         */
        @Nonnull public DateTime getExpirationTime() {
            return expirationTime;
        }

        /**
         * Set the expiration time of the metadata.
         * 
         * @param dateTime the new expiration time
         */
        public void setExpirationTime(@Nonnull final DateTime dateTime) {
            expirationTime = Constraint.isNotNull(dateTime, "Expiration time may not be null");
        }
        
        /**
         * Get the refresh trigger time of the metadata. 
         * 
         * @return the refresh trigger time
         */
        @Nonnull public DateTime getRefreshTriggerTime() {
            return refreshTriggerTime;
        }

        /**
         * Set the refresh trigger time of the metadata.
         * 
         * @param dateTime the new refresh trigger time
         */
        public void setRefreshTriggerTime(@Nonnull final DateTime dateTime) {
            refreshTriggerTime = Constraint.isNotNull(dateTime, "Refresh trigger time may not be null");
        }

        /**
         * Get the last time at which the entity's backing store data was accessed.
         * 
         * @return the time in milliseconds since the epoch
         */
        @Nonnull public DateTime getLastAccessedTime() {
            return lastAccessedTime;
        }
        
        /**
         * Record access of the entity's backing store data.
         */
        public void recordEntityAccess() {
            lastAccessedTime = new DateTime(ISOChronology.getInstanceUTC());
        }

        /**
         * Get the read-write lock instance which governs access to the entity's backing store data. 
         * 
         * @return the lock instance
         */
        @Nonnull public ReadWriteLock getReadWriteLock() {
            return readWriteLock;
        }
        
    }
    
    /**
     * Background maintenance task which cleans expired and idle metadata from the backing store, and removes
     * orphaned entity management data.
     */
    protected class BackingStoreCleanupSweeper extends TimerTask {
        
        /** Logger. */
        private final Logger log = LoggerFactory.getLogger(BackingStoreCleanupSweeper.class);

        /** {@inheritDoc} */
        public void run() {
            if (isDestroyed() || !isInitialized()) {
                // just in case the metadata resolver was destroyed before this task runs, 
                // or if it somehow is being called on a non-successfully-inited resolver instance.
                log.debug("BackingStoreCleanupSweeper will not run because: inited: {}, destroyed: {}",
                        isInitialized(), isDestroyed());
                return;
            }
            
            removeExpiredAndIdleMetadata();
        }

        /**
         *  Purge metadata which is either 1) expired or 2) (if {@link #isRemoveIdleEntityData()} is true) 
         *  which hasn't been accessed within the last {@link #getMaxIdleEntityData()} milliseconds.
         */
        private void removeExpiredAndIdleMetadata() {
            DateTime now = new DateTime(ISOChronology.getInstanceUTC());
            DateTime earliestValidLastAccessed = now.minus(getMaxIdleEntityData());
            
            DynamicEntityBackingStore backingStore = getBackingStore();
            Map<String, List<EntityDescriptor>> indexedDescriptors = backingStore.getIndexedDescriptors();
            
            for (String entityID : indexedDescriptors.keySet()) {
                EntityManagementData mgmtData = backingStore.getManagementData(entityID);
                Lock writeLock = mgmtData.getReadWriteLock().writeLock();
                try {
                    writeLock.lock();
                    
                    if (isRemoveData(mgmtData, now, earliestValidLastAccessed)) {
                        removeByEntityID(entityID, backingStore);
                        backingStore.removeManagementData(entityID);
                    }
                    
                } finally {
                    writeLock.unlock();
                }
            }
            
        }
        
        /**
         * Determine whether metadata should be removed based on expiration and idle time data.
         * 
         * @param mgmtData the management data instance for the entity
         * @param now the current time
         * @param earliestValidLastAccessed the earliest last accessed time which would be valid
         * 
         * @return true if the entity is expired or exceeds the max idle time, false otherwise
         */
        private boolean isRemoveData(EntityManagementData mgmtData, DateTime now, DateTime earliestValidLastAccessed) {
            if (isRemoveIdleEntityData() && mgmtData.getLastAccessedTime().isBefore(earliestValidLastAccessed)) {
                log.debug("Entity metadata exceeds maximum idle time, removing: {}", mgmtData.getEntityID());
                return true;
            } else if (now.isAfter(mgmtData.getExpirationTime())) {
                log.debug("Entity metadata is expired, removing: {}", mgmtData.getEntityID());
                return true;
            } else {
                return false;
            }
        }
        
    }

}
