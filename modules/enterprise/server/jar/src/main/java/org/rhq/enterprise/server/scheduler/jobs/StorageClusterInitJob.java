/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jiri Kremser
 */
public class StorageClusterInitJob extends AbstractStatefulJob {

    private Log log = LogFactory.getLog(StorageClusterInitJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        log.info("Preparing to run init job on storage cluster");
        boolean isStorageRunning =  LookupUtil.getStorageClientManager().init();
        if (isStorageRunning) {
            // cancel this job
            try {
                String clazzName = StorageClusterInitJob.class.getName();
                LookupUtil.getSchedulerBean().deleteJob(clazzName, clazzName);
            } catch (Exception e) {
                log.error("Cannot cancel storage cluster init job", e);
            }
        }
    }
}