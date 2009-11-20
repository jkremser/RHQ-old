/*
* RHQ Management Platform
* Copyright (C) 2005-2008 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package org.rhq.enterprise.server.plugin.pc.content;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Accumulates all the synced distros into a collection
 * @author Pradeep Kilambi
 */
public class DistributionSyncReport {
    private List<DistributionDetails> distros = new ArrayList<DistributionDetails>();
    private List<DistributionDetails> deletedDistros = new ArrayList<DistributionDetails>();
    private int repoId;

    public int getRepoId()
    {
        return repoId;
    }

    public void setRepoId(int repoIdIn)
    {
        this.repoId = repoIdIn;
    }

    public DistributionSyncReport(int repoIdIn) {
        repoId = repoIdIn;

    }

    public List<DistributionDetails> getDistributions() {
        return distros;
    }

    public void addDistro(DistributionDetails distroIn) {
        distros.add(distroIn);
    }

    public void addDistros(Collection<DistributionDetails> distrosIn) {
        distros.addAll(distrosIn);
    }

    public List<DistributionDetails> getDeletedDistributions() {
        return deletedDistros;
    }

    public void addDeletedDistro(DistributionDetails distroIn) {
        deletedDistros.add(distroIn);
    }

    public void addDeletedDistros(Collection<DistributionDetails> distrosIn) {
        deletedDistros.addAll(distrosIn);
    }
}
