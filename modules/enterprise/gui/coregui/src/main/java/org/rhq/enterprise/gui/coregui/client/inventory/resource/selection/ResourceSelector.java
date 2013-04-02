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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.LinkedHashMap;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypePluginTreeDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceSelector extends AbstractSelector<Resource, ResourceCriteria> {

    private ResourceType resourceTypeFilter;
    private boolean forceResourceTypeFilter;
    private boolean displayResourceTypeFilter = true;
    private IPickTreeItem typeSelectItem;

    public ResourceSelector() {
        this(null, false);
    }

    public ResourceSelector(ResourceType resourceTypeFilter, boolean forceResourceTypeFilter) {
        super();
        this.resourceTypeFilter = resourceTypeFilter;
        this.forceResourceTypeFilter = forceResourceTypeFilter;
    }

    protected DynamicForm getAvailableFilterForm() {
        if (null == availableFilterForm) {
            availableFilterForm = new DynamicForm();
            availableFilterForm.setNumCols(6);
            availableFilterForm.setWidth("75%");
            final TextItem search = new TextItem("search", MSG.common_title_search());
            final SelectItem categorySelect;

            typeSelectItem = new IPickTreeItem("type", MSG.common_title_type());
            typeSelectItem.setDataSource(new ResourceTypePluginTreeDataSource(false));
            typeSelectItem.setValueField("id");
            typeSelectItem.setCanSelectParentItems(true);
            typeSelectItem.setLoadDataOnDemand(false);
            typeSelectItem.setEmptyMenuMessage(MSG.common_msg_loading());
            typeSelectItem.setShowIcons(false);

            if (this.forceResourceTypeFilter) {
                typeSelectItem.setDisabled(true);
            }
            if (!isDisplayResourceTypeFilter()) {
                typeSelectItem.setVisible(false);
            }

            categorySelect = new SelectItem("category", MSG.common_title_category());
            LinkedHashMap<String, String> valueMap = buildResourceCategoryValueMap();
            categorySelect.setValueMap(valueMap);
            categorySelect.setAllowEmptyValue(true);

            availableFilterForm.setItems(search, categorySelect, typeSelectItem);
        }

        return availableFilterForm;
    }

    private LinkedHashMap<String, String> buildResourceCategoryValueMap() {
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        ResourceCategory[] categories = ResourceCategory.values();
        for (ResourceCategory category : categories) {
            valueMap.put(category.name(), category.getDisplayName());
        }
        return valueMap;
    }

    protected RPCDataSource<Resource, ResourceCriteria> getDataSource() {
        if (null == datasource) {
            datasource = new SelectedResourceDataSource();
        }

        return datasource;
    }

    // TODO: Until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed, avoid AdvancedCriteria and always
    // use server-side fetch and simple criteria. When fixed, use the commented version below. Also see
    // ResourceDataSource.
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        String type = availableFilterForm.getValueAsString("type");
        String category = (String) availableFilterForm.getValue("category");
        Criteria criteria = new Criteria();
        if (null != search) {
            criteria.addCriteria(NAME.propertyName(), search);
        }
        if (null != type) {
            // If type is a number its a typeId, otherwise a plugin name
            try {
                Integer.parseInt(type);
                criteria.addCriteria(TYPE.propertyName(), type);
            } catch (NumberFormatException nfe) {
                criteria.addCriteria(PLUGIN.propertyName(), type);
            }
        }
        if (null != category) {
            criteria.addCriteria(CATEGORY.propertyName(), category);
        }

        return criteria;
    }

    //  protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
    //  String search = (String) availableFilterForm.getValue("search");
    //  String type = availableFilterForm.getValueAsString("type");
    //  String category = (String) availableFilterForm.getValue("category");
    //  ArrayList<Criterion> criteria = new ArrayList<Criterion>(3);
    //  if (null != search) {
    //      criteria.add(new Criterion(NAME.propertyName(), OperatorId.CONTAINS, search));
    //  }
    //  if (null != type) {
    //      // If type is a number its a typeId, otherwise a plugin name
    //      try {
    //          Integer.parseInt(type);
    //          criteria.add(new Criterion(TYPE.propertyName(), OperatorId.EQUALS, type));
    //      } catch (NumberFormatException nfe) {
    //          criteria.add(new Criterion(PLUGIN.propertyName(), OperatorId.EQUALS, type));
    //      }
    //  }
    //  if (null != category) {
    //      criteria.add(new Criterion(CATEGORY.propertyName(), OperatorId.EQUALS, category));
    //  }
    //  AdvancedCriteria latestCriteria = new AdvancedCriteria(OperatorId.AND, criteria.toArray(new Criterion[criteria
    //      .size()]));
    //
    //  return latestCriteria;
    //}

    @Override
    protected String getItemTitle() {
        return "resource";
    }

    @Override
    protected HoverCustomizer getNameHoverCustomizer() {
        return new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getAncestryHoverHTML(listGridRecord, 0);
            }
        };
    }

    @Override
    protected boolean supportsNameHoverCustomizer() {
        return true;
    }

    private class SelectedResourceDataSource extends ResourceDatasource {

        @Override
        protected ResourceCriteria getFetchCriteria(final DSRequest request) {
            // if specified seed with an initial type filter
            if (null != ResourceSelector.this.resourceTypeFilter) {
                ResourceSelector.this.typeSelectItem.setValue(resourceTypeFilter.getId());
                request.getCriteria().addCriteria(TYPE.propertyName(), String.valueOf(resourceTypeFilter.getId()));
                ResourceSelector.this.resourceTypeFilter = null;
            }

            ResourceCriteria result = super.getFetchCriteria(request);

            // additional return data
            result.fetchResourceType(true);

            return result;
        }

    }

    public boolean isDisplayResourceTypeFilter() {
        return displayResourceTypeFilter;
    }

    public void setDisplayResourceTypeFilter(boolean displayResourceTypeFilter) {
        this.displayResourceTypeFilter = displayResourceTypeFilter;
    }
}
