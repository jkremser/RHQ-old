/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.plugin.pc.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.content.ContentSyncStatus;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.enterprise.server.content.RepoManagerLocal;

/**
 * Container class to hold the classes required to track the progress of a ContentProvider Sync.
 * @author mmccune
 *
 */
public class SyncTracker {

    private final Log log = LogFactory.getLog(SyncTracker.class);

    RepoManagerLocal repoManager;
    private int repoId;
    private RepoSyncResults repoSyncResults;
    private int packageSyncCount;

    private boolean started = false;
    private int totalWork = -1;
    private int finishedWork = -1;

    /**
     * @param repoSyncResults
     * @param progressWatcher
     */
    public SyncTracker(RepoManagerLocal repoManagerIn, RepoSyncResults repoSyncResultsIn) {
        super();
        this.repoId = repoSyncResultsIn.getRepo().getId();
        this.repoSyncResults = repoSyncResultsIn;
        this.repoManager = repoManagerIn;
    }

    public void addPackageBitsWork(ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        this.addWork(sw.getPackageBitsWeight() * this.getPackageSyncCount());
    }

    public void finishPackageBitsWork(ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        this.finishWork(sw.getPackageBitsWeight() * this.getPackageSyncCount());
    }

    public void addDistroMetadataWork(int distCount, ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        this.addWork(sw.getDistribtutionMetadataWeight() * distCount);
    }

    public void finishDistroMetadataWork(int distCount, ContentProvider provider) {
        SyncProgressWeight sw = provider.getSyncProgressWeight();
        this.finishWork(sw.getDistribtutionMetadataWeight() * distCount);

    }

    /**
     * @return the repoSyncResults
     */
    public RepoSyncResults getRepoSyncResults() {
        return repoSyncResults;
    }

    /**
     * Set the RepoSyncResults
     * @param syncResultsIn
     */
    public void setRepoSyncResults(RepoSyncResults syncResultsIn) {
        this.repoSyncResults = syncResultsIn;
    }

    /**
     * Set the Results field on the RepoSyncResults.

     * @param resultsIn to set
     */
    public void setResults(String resultsIn) {
        this.repoSyncResults.setResults(resultsIn);

    }

    /**
     * passthrough to RepoSyncResults.setStatus()
     * 
     * @param statusIn
     */
    public void setStatus(ContentSyncStatus statusIn) {
        this.repoSyncResults.setStatus(statusIn);

    }

    /**
     * @return the packageSyncCount
     */
    public int getPackageSyncCount() {
        return packageSyncCount;
    }

    /**
     * @param packageSyncCount the packageSyncCount to set
     */
    public void setPackageSyncCount(int packageSyncCount) {
        this.packageSyncCount = packageSyncCount;
    }

    /**
     * RepoId we are tracking
     * @return int repoId
     */
    public int getRepoId() {
        return this.repoId;
    }

    /**
     * Store the percentage complete to the database
     * 
     */
    public void persistResults() {
        repoSyncResults.setPercentComplete(new Long(this.getPercentComplete()));
        this.setRepoSyncResults(repoManager.mergeRepoSyncResults(repoSyncResults));
    }

    /**
     * Start watching the progress of a given amount of work.
     */
    public void start() {
        totalWork = 0;
        finishedWork = 0;
        started = true;
    }

    /**
     * Get the percentage complete of the total work specified.
     * @return float 0-100% of the amount of work copleted.  integer so no decimal points.
     * @throws IllegalStateException if this ProgressWatcher has not been started yet.
     */
    public int getPercentComplete() throws IllegalStateException {
        if (!started) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                + " not started yet. call start() to set progress to 0 and start watching.");
        }
        if (totalWork == 0) {
            return 0;
        } else {
            float percentComp = (((float) finishedWork / (float) totalWork) * 100);
            return (int) percentComp;
        }

    }

    /**
     * Set the total amount of work to be completed.
     * 
     * @param totalWorkIn to set.
     */
    public void setTotalWork(int totalWorkIn) {
        this.totalWork = totalWorkIn;
    }

    /**
     * Add a unit of work to be completed.
     * 
     * @param workToAdd 
     */
    public void addWork(int workToAdd) {
        totalWork = totalWork + workToAdd;
        log.debug("    addWork() - ADD   : " + workToAdd + " Total work: [" + totalWork + "] Finished work: "
            + finishedWork);
    }

    /**
     * Indicate that a # of work units has been completed.
     * 
     * @param workToRemove
     */
    public void finishWork(int workToRemove) {
        if (!started) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                + " not started yet. call start() to set progress to 0 and start watching.");
        }
        finishedWork += workToRemove;
        log.debug("    finishWork() - REM: " + workToRemove + " Total work: [" + totalWork + "] Finished work: "
            + finishedWork);
    }

    /**
     * Indicate this ProgressWatcher is finished watching.
     */
    public void stop() {
        this.started = false;
    }

    /**
     * Reset the ProgressWatcher to zero.
     */
    public void resetToZero() {
        stop();
        start();
    }

}
