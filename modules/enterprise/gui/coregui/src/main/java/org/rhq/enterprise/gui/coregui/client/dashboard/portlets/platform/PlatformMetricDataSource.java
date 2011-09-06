/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.platform;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;

/**
 * @author Greg Hinkle
 */
public class PlatformMetricDataSource extends ResourceDatasource {

    PlatformSummaryPortlet view;

    public PlatformMetricDataSource(PlatformSummaryPortlet view) {
        super();
        this.view = view;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        return new ArrayList<DataSourceField>();
    }

    @Override
    public ListGridRecord copyValues(Resource from) {
        ListGridRecord record = super.copyValues(from);

        view.loadMetricsForResource(from, record);
        return record;
    }
}
