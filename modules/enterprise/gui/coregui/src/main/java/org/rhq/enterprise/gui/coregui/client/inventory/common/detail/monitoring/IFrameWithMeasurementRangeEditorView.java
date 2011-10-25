/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.monitoring;

import com.smartgwt.client.types.ContentsType;

import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class IFrameWithMeasurementRangeEditorView extends LocatableVLayout implements RefreshableView {

    UserPreferencesMeasurementRangeEditor editor;
    LocatableHTMLPane iframe;
    
    public IFrameWithMeasurementRangeEditorView(String locatorId, final String url) {
        super(locatorId);

        iframe = new LocatableHTMLPane(extendLocatorId("jsf")); 
        iframe.setContentsURL(url);
        iframe.setContentsType(ContentsType.PAGE);
        iframe.setWidth100();
        
        addMember(iframe);

        LocatableToolStrip footer = new LocatableToolStrip("toolStrip");
        footer.setWidth100();
        addMember(footer);
                
        editor = new UserPreferencesMeasurementRangeEditor(extendLocatorId("range")) {
            @Override
            public void setMetricRangeProperties(MetricRangePreferences prefs) {
                super.setMetricRangeProperties(prefs);
                iframe.setContentsURL(url);
            }
        };
        
        footer.addMember(editor);
    }
    
    public void refresh() {
        editor.refresh(null);
    }
}
