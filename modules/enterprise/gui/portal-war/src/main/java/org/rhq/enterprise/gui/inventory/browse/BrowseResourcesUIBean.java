package org.rhq.enterprise.gui.inventory.browse;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.ResourceNameDisambiguatingPagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BrowseResourcesUIBean extends PagedDataTableUIBean {
    protected final Log log = LogFactory.getLog(BrowseResourcesUIBean.class);

    public static final String MANAGED_BEAN_NAME = "BrowseResourcesUIBean";

    private String filter;
    private ResourceCategory category;

    private static final IntExtractor<ResourceComposite> RESOURCE_ID_EXTRATOR = new IntExtractor<ResourceComposite>() {
        public int extract(ResourceComposite object) {
            return object.getResource().getId();
        }
    };

    public BrowseResourcesUIBean() {
        String subtab = FacesContextUtility.getOptionalRequestParameter("subtab", "").toLowerCase();
        if (subtab.equals("platform")) {
            category = ResourceCategory.PLATFORM;
        } else if (subtab.equals("server")) {
            category = ResourceCategory.SERVER;
        } else if (subtab.equals("service")) {
            category = ResourceCategory.SERVICE;
        }

        filter = FacesContextUtility.getOptionalRequestParameter("filter");
    }

    public String getFilter() {
        return this.filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public ResourceCategory getCategory() {
        return this.category;
    }

    @Override
    public synchronized DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.BrowseResources, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private class ResultsDataModel extends ResourceNameDisambiguatingPagedListDataModel<ResourceComposite> {

        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName, true);
        }

        public PageList<ResourceComposite> fetchDataForPage(PageControl pc) {
            String filter = getFilter();
            ResourceCategory category = getCategory();

            ResourceCriteria criteria = new ResourceCriteria();
            criteria.setPageControl(pc);
            if (filter != null && !filter.equals("")) {
                criteria.addFilterName(filter);
            }
            criteria.addFilterResourceCategory(category);
            criteria.fetchParentResource(true);

            PageList<ResourceComposite> results;
            results = resourceManager.findResourceCompositesByCriteria(getSubject(), criteria);
            return results;
        }

        protected IntExtractor<ResourceComposite> getResourceIdExtractor() {
            return RESOURCE_ID_EXTRATOR;
        }
    }

    public String uninventorySelectedResources() {
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            String[] selectedResources = getSelectedItems();
            int[] resourceIds = StringUtility.getIntArray(selectedResources);

            resourceManager.uninventoryResources(subject, resourceIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Uninventoried selected resources");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to uninventory selected resources", e);
        }

        return "success";
    }

    private String[] getSelectedItems() {
        return FacesContextUtility.getRequest().getParameterValues("selectedItems");
    }

    public class Suggestion {
        String label;
        String value;

        public Suggestion(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public List<Suggestion> autocomplete(Object suggest) {
        String currentInputText = (String) suggest;
        List<Suggestion> results = new ArrayList<Suggestion>();
        // offer suggestions based on currentInputText
        return results;
    }

}
