/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ClusterFlyweight;
import org.rhq.core.domain.resource.group.composite.ClusterKeyFlyweight;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * This is the view that renders the left hand tree for groups.
 * There are three main types of nodes in the group tree:
 * 1. Cluster Node - This represents a single aggregate resource that is of a specific resource type.
 *                   If a group has members with one or more resources with an identical resource key and type,
 *                   those identical resources are represented with one cluster node. Each cluster node
 *                   is associated with a resource type and an unique cluster key.
 * 2. Auto Type Group Node - This is a folder node whose children are all of a specific resource type.
 *                           The children are typically cluster nodes. An example of this kind of node is
 *                           "WARs", where there can be many different WARs deployed on an individual member resource
 *                           but a WAR can be clustered (copied) across many member resources. Each auto type group node
 *                           is associated with a resource type but they do not have cluster keys.
 * 3. Subcategory Node these are simply nodes that group other kinds of nodes. Plugin developers define subcategories
 *                     in plugin descriptors to organize resource types. Subcategories are not associated with any
 *                     particular resource type and do not have cluster keys.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class ResourceGroupTreeView extends LocatableVLayout implements BookmarkableView {

    private static final String FAKE_ROOT_ID = "__fakeRoot__"; // id of the parent node of our real root node

    private TreeGrid treeGrid;
    private ViewId currentViewId;
    private Map<Integer, ResourceType> typeMap;
    private ResourceGroupTreeContextMenu contextMenu;

    // the root (top of tree) compat or mixed group
    private ResourceGroup rootResourceGroup;

    // the root (top of tree) compat or mixed group id (may bet set prior to rootResourceGroup fetch)
    private int rootGroupId;

    // the currently selected tree node
    private String currentNodeId;

    // the currently selected group backing the tree node
    private ResourceGroup currentGroup;

    private Comparator<ResourceGroupEnhancedTreeNode> treeNodeComparator = new Comparator<ResourceGroupEnhancedTreeNode>() {
        @Override
        public int compare(ResourceGroupEnhancedTreeNode o1, ResourceGroupEnhancedTreeNode o2) {
            // folders always come before leaf nodes
            boolean o1IsFolder = o1.isFolderNode();
            boolean o2IsFolder = o2.isFolderNode();
            if (o1IsFolder != o2IsFolder) {
                return o1IsFolder ? -1 : 1;
            } else if (o1IsFolder) {
                // subcategory and autoTypeGroup nodes (i.e. "folder icons") come before cluster type group nodes
                boolean o1IsClusterNode = o1.getClusterKey() != null;
                boolean o2IsClusterNode = o2.getClusterKey() != null;
                if (o1IsClusterNode != o2IsClusterNode) {
                    return !o1IsClusterNode ? -1 : 1;
                }
            }

            // the two nodes are either both leaves or both folders; sort by name
            String s1 = o1.getName();
            String s2 = o2.getName();
            return s1.compareToIgnoreCase(s2);
        }
    };

    public ResourceGroupTreeView(String locatorId) {
        super(locatorId);

        setWidth(250);
        setHeight100();

        setShowResizeBar(true);
    }

    @Override
    protected void onInit() {
        super.onInit();

        treeGrid = new CustomResourceGroupTreeGrid(extendLocatorId("groupTree"));
        treeGrid.setWidth100();
        treeGrid.setHeight100();
        treeGrid.setAnimateFolders(false);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);
        treeGrid.setShowRollOver(false);
        treeGrid.setShowHeader(false);
        treeGrid.setLeaveScrollbarGap(false);
        treeGrid.setOpenerImage("resources/dir.png");
        treeGrid.setOpenerIconSize(16);
        treeGrid.setCanHover(true);
        treeGrid.setShowHover(true);
        treeGrid.setHoverWidth(250);
        treeGrid.setHoverWrap(true);
        treeGrid.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String tooltip = record.getAttribute(ResourceGroupEnhancedTreeNode.TOOLTIP_KEY);
                return tooltip;
            }
        });

        contextMenu = new ResourceGroupTreeContextMenu(extendLocatorId("contextMenu"));
        treeGrid.setContextMenu(contextMenu);

        treeGrid.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                // stop the browser right-click menu
                event.cancel();

                // don't select the node on a right click, since we're not navigating to it, and
                // re-select the current node if necessary                
                ResourceGroupEnhancedTreeNode contextNode = (ResourceGroupEnhancedTreeNode) event.getNode();

                if (null != currentNodeId) {
                    TreeNode currentNode = treeGrid.getTree().findById(currentNodeId);
                    if (!contextNode.equals(currentNode)) {
                        treeGrid.deselectRecord(contextNode);
                        treeGrid.selectRecord(currentNode);
                    }
                }

                // only show the context menu for cluster nodes and our top root node
                if (contextNode.isCompatibleGroupTopNode() || contextNode.isAutoClusterNode()) {
                    contextMenu.showContextMenu(treeGrid.getTree(), contextNode);
                }
            }
        });

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {

            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (!selectionEvent.isRightButtonDown() && selectionEvent.getState()) {

                    ResourceGroupEnhancedTreeNode newNode = (ResourceGroupEnhancedTreeNode) selectionEvent.getRecord();
                    TreeNode currentNode = (null != currentNodeId) ? treeGrid.getTree().findById(currentNodeId) : null;

                    // if re-selecting the current node just return, otherwise deselect the currently selected node
                    if (null != currentNode) {
                        if (newNode.equals(currentNode)) {
                            return;
                        } else {
                            treeGrid.deselectRecord(currentNode);
                        }
                    }

                    com.allen_sauer.gwt.log.client.Log.debug("Node selected in tree: " + newNode);

                    if (newNode.isCompatibleGroupTopNode()) {
                        currentNodeId = newNode.getID();
                        com.allen_sauer.gwt.log.client.Log.debug("Selecting compatible group [" + currentNodeId
                            + "]...");
                        String viewPath = LinkManager.getResourceGroupLink(Integer.parseInt(currentNodeId));
                        String currentViewPath = History.getToken();
                        if (!currentViewPath.startsWith(viewPath.substring(1))) {
                            CoreGUI.goToView(viewPath, true);
                        } else {
                            // this should not be necessary but for otherwise the top node does not always show selected
                            treeGrid.markForRedraw();
                        }

                    } else if (newNode.isAutoClusterNode()) {
                        ClusterKey key = newNode.getClusterKey();
                        com.allen_sauer.gwt.log.client.Log.debug("Selecting autocluster group [" + key + "]...");
                        currentNodeId = newNode.getID();
                        // the user selected a cluster node - let's switch to that cluster group view                            
                        selectClusterGroup(key);

                    } else if (newNode.isMixedGroupTopNode()) {
                        currentNodeId = newNode.getID();
                        com.allen_sauer.gwt.log.client.Log.debug("Selecting mixed group [" + currentNodeId + "]...");

                    } else {
                        // a subcategory node, deselect and reselect the current node
                        treeGrid.deselectRecord(newNode);
                        if (null != currentNode) {
                            treeGrid.selectRecord(currentNode);
                        }
                    }
                }

                return;
            }
        });

        addMember(this.treeGrid);
    }

    public void setSelectedGroup(final int groupId, boolean isAutoCluster) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.addFilterVisible(!isAutoCluster);
        criteria.fetchResourceType(true);

        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_tree_common_loadFailed_group(String.valueOf(groupId)), caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    if (result.isEmpty()) {
                        // This means either that a group with the specified id does not exist or that the user does is
                        // not authorized to view the group. In either case, just return, and let
                        // ResourceGroupDetailView.loadSelectedItem() handle emitting an error message.
                        return;
                    }
                    ResourceGroup group = result.get(0);
                    ResourceGroupTreeView.this.currentGroup = group;

                    GroupCategory groupCategory = group.getGroupCategory();
                    switch (groupCategory) {
                    case MIXED:
                        // For mixed groups, there will only ever be one item in the tree, even if the group is recursive.
                        // This is because mixed groups don't normally have clustered/identical resources across members
                        // so there is no attempt here to build auto cluster nodes.
                        ResourceGroupTreeView.this.rootResourceGroup = group;
                        ResourceGroupTreeView.this.rootGroupId = rootResourceGroup.getId();
                        ResourceGroupEnhancedTreeNode fakeRoot = new ResourceGroupEnhancedTreeNode("fakeRootNode");
                        String groupName = group.getName();
                        String escapedGroupName = StringUtility.escapeHtml(groupName);
                        ResourceGroupEnhancedTreeNode rootNode = new ResourceGroupEnhancedTreeNode(escapedGroupName);
                        String icon = ImageManager.getGroupIcon(GroupCategory.MIXED);
                        rootNode.setIcon(icon);
                        rootNode.setID(String.valueOf(rootResourceGroup.getId()));
                        fakeRoot.setID(FAKE_ROOT_ID);
                        rootNode.setParentID(fakeRoot.getID());
                        fakeRoot.setChildren(new ResourceGroupEnhancedTreeNode[] { rootNode });
                        Tree tree = new Tree();
                        tree.setRoot(fakeRoot);
                        treeGrid.setData(tree);
                        treeGrid.selectRecord(rootNode);
                        break;

                    case COMPATIBLE:
                        if (group.getClusterResourceGroup() == null) {
                            // This is a straight up compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group;
                        } else {
                            // This is a cluster group beneath a real recursive compatible group.
                            ResourceGroupTreeView.this.rootResourceGroup = group.getClusterResourceGroup();
                        }
                        loadGroup(ResourceGroupTreeView.this.rootResourceGroup.getId());
                        break;
                    }
                }
            });
    }

    private void loadGroup(int groupId) {
        if (groupId == this.rootGroupId) {
            // Still looking at the same compat-recursive tree

            ResourceGroupEnhancedTreeNode selectedNode;
            if (this.currentGroup.getClusterKey() != null) {
                // a child cluster node leaf was selected
                selectedNode = (ResourceGroupEnhancedTreeNode) treeGrid.getTree().find(
                    ResourceGroupEnhancedTreeNode.CLUSTER_KEY, this.currentGroup.getClusterKey());
            } else {
                // the top root node, representing the group itself, was selected
                selectedNode = (ResourceGroupEnhancedTreeNode) treeGrid.getTree().findById(
                    String.valueOf(this.currentGroup.getId()));
            }

            if (selectedNode != null) {
                TreeNode[] parents = treeGrid.getTree().getParents(selectedNode);
                treeGrid.getTree().openFolders(parents);
                treeGrid.getTree().openFolder(selectedNode);
                treeGrid.selectRecord(selectedNode);
            }
        } else {
            this.rootGroupId = groupId;
            GWTServiceLookup.getClusterService().getClusterTree(groupId, new AsyncCallback<ClusterFlyweight>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_loadFailed_groupTree(), caught);
                }

                public void onSuccess(ClusterFlyweight result) {
                    loadTreeTypes(result);
                }
            });
        }
    }

    private void loadTreeTypes(final ClusterFlyweight root) {
        Set<Integer> typeIds = new HashSet<Integer>();
        typeIds.add(this.rootResourceGroup.getResourceType().getId());
        getTreeTypes(root, typeIds);

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(typeIds.toArray(new Integer[typeIds.size()]),
            EnumSet.of(ResourceTypeRepository.MetadataType.subCategory),
            new ResourceTypeRepository.TypesLoadedCallback() {

                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    ResourceGroupTreeView.this.typeMap = types;
                    loadTree(root);
                }
            });
    }

    private void selectClusterGroup(ClusterKey key) {
        GWTServiceLookup.getClusterService().createAutoClusterBackingGroup(key, true,
            new AsyncCallback<ResourceGroup>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_tree_common_createFailed_autoCluster(), caught);
                }

                public void onSuccess(ResourceGroup result) {
                    renderAutoCluster(result);
                }
            });
    }

    private void renderAutoCluster(ResourceGroup backingGroup) {
        String viewPath = ResourceGroupDetailView.AUTO_CLUSTER_VIEW + "/" + backingGroup.getId();
        String currentViewPath = History.getToken();
        if (!currentViewPath.startsWith(viewPath)) {
            CoreGUI.goToView(viewPath);
        }
    }

    private void loadTree(ClusterFlyweight root) {
        ClusterKey rootKey = new ClusterKey(root.getGroupId());
        ResourceGroupEnhancedTreeNode fakeRoot = new ResourceGroupEnhancedTreeNode("fakeRootNode");
        fakeRoot.setID(FAKE_ROOT_ID);

        String groupName = rootResourceGroup.getName();
        String escapedGroupName = StringUtility.escapeHtml(groupName);
        ResourceGroupEnhancedTreeNode rootNode = new ResourceGroupEnhancedTreeNode(escapedGroupName);
        rootNode.setID(rootKey.getKey());
        rootNode.setParentID(fakeRoot.getID());

        ResourceType rootResourceType = typeMap.get(rootResourceGroup.getResourceType().getId());
        rootNode.setResourceType(rootResourceType);

        String icon = ImageManager.getClusteredResourceIcon(rootResourceType.getCategory());
        rootNode.setIcon(icon);

        fakeRoot.setChildren(new ResourceGroupEnhancedTreeNode[] { rootNode });

        loadTree(rootNode, root, rootKey);

        Tree tree = new Tree();
        tree.setRoot(fakeRoot);
        org.rhq.enterprise.gui.coregui.client.util.TreeUtility.printTree(tree);

        treeGrid.setData(tree);
        treeGrid.getTree().openFolder(rootNode);
        treeGrid.selectRecord(rootNode);
    }

    public void loadTree(ResourceGroupEnhancedTreeNode parentNode, ClusterFlyweight parentClusterGroup,
        ClusterKey parentKey) {
        if (!parentClusterGroup.getChildren().isEmpty()) {
            // First pass - group the children by type.
            Map<ResourceType, List<ClusterFlyweight>> childrenByType = new HashMap<ResourceType, List<ClusterFlyweight>>();
            for (ClusterFlyweight child : parentClusterGroup.getChildren()) {
                ClusterKeyFlyweight keyFlyweight = child.getClusterKey();

                ResourceType type = this.typeMap.get(keyFlyweight.getResourceTypeId());
                List<ClusterFlyweight> children = childrenByType.get(type);
                if (children == null) {
                    children = new ArrayList<ClusterFlyweight>();
                    childrenByType.put(type, children);
                }
                children.add(child);
            }

            // Second pass - process each of the sets of like-typed children created in the first pass.
            List<ResourceGroupEnhancedTreeNode> childNodes = new ArrayList<ResourceGroupEnhancedTreeNode>();
            Map<String, ResourceGroupEnhancedTreeNode> subCategoryNodesByName = new HashMap<String, ResourceGroupEnhancedTreeNode>();
            Map<String, List<ResourceGroupEnhancedTreeNode>> subCategoryChildrenByName = new HashMap<String, List<ResourceGroupEnhancedTreeNode>>();
            for (ResourceType childType : childrenByType.keySet()) {
                List<ClusterFlyweight> children = childrenByType.get(childType);
                List<ResourceGroupEnhancedTreeNode> nodesByType = new ArrayList<ResourceGroupEnhancedTreeNode>();
                for (ClusterFlyweight child : children) {
                    ResourceGroupEnhancedTreeNode node = createClusterGroupNode(parentKey, childType, child);
                    nodesByType.add(node);

                    if (!child.getChildren().isEmpty()) {
                        ClusterKey key = node.getClusterKey();
                        loadTree(node, child, key); // recurse
                    }
                }

                // Insert an autoTypeGroup node if the type is not a singleton.
                if (!childType.isSingleton()) {
                    // This will override the parent IDs of all nodesByType nodes with the auto group node ID that is being created
                    ResourceGroupEnhancedTreeNode autoTypeGroupNode = createAutoTypeGroupNode(parentKey, childType,
                        nodesByType);
                    nodesByType.clear();
                    nodesByType.add(autoTypeGroupNode);
                }

                // Insert subcategory node(s) if the type has a subcategory.
                ResourceSubCategory subcategory = childType.getSubCategory();
                if (subcategory != null) {
                    ResourceGroupEnhancedTreeNode lastSubcategoryNode = null;

                    ResourceSubCategory currentSubCategory = subcategory;
                    boolean currentSubcategoryNodeCreated = false;
                    do {
                        ResourceGroupEnhancedTreeNode currentSubcategoryNode = subCategoryNodesByName
                            .get(currentSubCategory.getName());
                        if (currentSubcategoryNode == null) {
                            // This node represents a subcategory. It is not associated with any specific resource type
                            // or cluster key - it is merely a way plugin developers organize different resource types into groups.
                            currentSubcategoryNode = new ResourceGroupEnhancedTreeNode(currentSubCategory.getName());
                            currentSubcategoryNode.setTitle(currentSubCategory.getDisplayName()); // subcategory names are normally plural already, no need to pluralize them
                            currentSubcategoryNode.setIsFolder(true);
                            currentSubcategoryNode.setID("cat" + currentSubCategory.getName());
                            currentSubcategoryNode.setParentID(parentKey.getKey());
                            subCategoryNodesByName.put(currentSubCategory.getName(), currentSubcategoryNode);
                            subCategoryChildrenByName.put(currentSubCategory.getName(),
                                new ArrayList<ResourceGroupEnhancedTreeNode>());

                            if (currentSubCategory.getParentSubCategory() == null) {
                                // It's a root subcategory - add a node for it to the tree.
                                childNodes.add(currentSubcategoryNode);
                            }

                            currentSubcategoryNodeCreated = true;
                        }

                        if (lastSubcategoryNode != null) {
                            List<ResourceGroupEnhancedTreeNode> currentSubcategoryChildren = subCategoryChildrenByName
                                .get(currentSubcategoryNode.getName());
                            // make sure we re-parent the child so it is under the subcategory folder
                            for (ResourceGroupEnhancedTreeNode currentSubcategoryChild : currentSubcategoryChildren) {
                                currentSubcategoryChild.setParentID(currentSubcategoryNode.getID());
                            }
                            currentSubcategoryChildren.add(lastSubcategoryNode);
                        }
                        lastSubcategoryNode = currentSubcategoryNode;
                    } while (currentSubcategoryNodeCreated
                        && (currentSubCategory = currentSubCategory.getParentSubCategory()) != null);

                    List<ResourceGroupEnhancedTreeNode> subcategoryChildren = subCategoryChildrenByName.get(subcategory
                        .getName());
                    subcategoryChildren.addAll(nodesByType);
                } else {
                    childNodes.addAll(nodesByType);
                }
            }

            for (String subcategoryName : subCategoryNodesByName.keySet()) {
                ResourceGroupEnhancedTreeNode subcategoryNode = subCategoryNodesByName.get(subcategoryName);
                List<ResourceGroupEnhancedTreeNode> subcategoryChildren = subCategoryChildrenByName
                    .get(subcategoryName);
                // make sure the parent for the subcat children are referring to the parent subcat node
                for (ResourceGroupEnhancedTreeNode subcatChild : subcategoryChildren) {
                    subcatChild.setParentID(subcategoryNode.getID());
                }
                createSortedArray(subcategoryChildren);
                subcategoryNode.setChildren(createSortedArray(subcategoryChildren));
            }

            parentNode.setChildren(createSortedArray(childNodes));
        }
    }

    private ResourceGroupEnhancedTreeNode[] createSortedArray(List<ResourceGroupEnhancedTreeNode> list) {
        Collections.sort(list, this.treeNodeComparator);
        return list.toArray(new ResourceGroupEnhancedTreeNode[list.size()]);
    }

    private ResourceGroupEnhancedTreeNode createClusterGroupNode(ClusterKey parentKey, ResourceType type,
        ClusterFlyweight child) {

        // This node represents one type of resource that has 1 or more individual resources as members in the group.
        // It will be associated with both a resource type and a cluster key.
        ResourceGroupEnhancedTreeNode node = new ResourceGroupEnhancedTreeNode(child.getName());

        ClusterKeyFlyweight keyFlyweight = child.getClusterKey();
        ClusterKey key = new ClusterKey(parentKey, keyFlyweight.getResourceTypeId(), keyFlyweight.getResourceKey());
        String id = key.getKey();
        String parentId = parentKey.getKey();
        node.setID(id);
        node.setParentID(parentId);
        node.setClusterKey(key);
        node.setResourceType(type);
        node.setIsFolder(!child.getChildren().isEmpty());

        int memberCount = child.getMembers();
        int clusterSize = child.getClusterSize();

        if (memberCount < clusterSize) {
            // it appears one or more individual group members doesn't have a resource with the given cluster key
            // label the tree node so the user knows this cluster node is not representative of the entire group membership
            double percentage = (double) memberCount / (double) clusterSize;
            String percentageStr = NumberFormat.getFormat("0%").format(percentage);
            String title = child.getName() + " <span style=\"color: red; font-style: italic\">(" + percentageStr
                + ")</span>";
            node.setTitle(title);

            // "1 out of 2 group members have "foo" child resources"
            node.setTooltip(MSG.group_tree_partialClusterTooltip(String.valueOf(memberCount), String
                .valueOf(clusterSize), child.getName()));
        }

        return node;
    }

    private ResourceGroupEnhancedTreeNode createAutoTypeGroupNode(ClusterKey parentKey, ResourceType type,
        List<ResourceGroupEnhancedTreeNode> memberNodes) {

        // This node represents a group of resources all of the same type but have different resource keys -
        // in other words, individual members of the group have multiple resources of this type (for example,
        // an auto type group node of type WAR means our group members each have multiple WARs deployed to them,
        // so this node represents the parent to all the different WARs cluster nodes).
        // This node will be associated with only a resource type (not a cluster key)
        String name = StringUtility.pluralize(type.getName());
        ResourceGroupEnhancedTreeNode autoTypeGroupNode = new ResourceGroupEnhancedTreeNode(name);
        String parentId = parentKey.getKey();
        String autoTypeGroupNodeId = "rt" + String.valueOf(type.getId());
        autoTypeGroupNode.setID(autoTypeGroupNodeId);
        autoTypeGroupNode.setParentID(parentId);
        autoTypeGroupNode.setResourceType(type); // notice this node has a resource type, but not a cluster key
        autoTypeGroupNode.setIsFolder(true);
        for (ResourceGroupEnhancedTreeNode memberNode : memberNodes) {
            memberNode.setParentID(autoTypeGroupNodeId);
        }
        autoTypeGroupNode.setChildren(createSortedArray(memberNodes));
        return autoTypeGroupNode;
    }

    public void renderView(ViewPath viewPath) {
        currentViewId = viewPath.getCurrent();
        if (this.currentViewId != null) {
            String currentViewIdPath = currentViewId.getPath();
            if ("AutoCluster".equals(currentViewIdPath)) {
                // Move the currentViewId to the ID portion to play better with other code
                currentViewId = viewPath.getNext();
                String clusterGroupIdString = currentViewId.getPath();
                Integer clusterGroupId = Integer.parseInt(clusterGroupIdString);
                setSelectedGroup(clusterGroupId, true);
            } else {
                String groupIdString = currentViewId.getPath();
                int groupId = Integer.parseInt(groupIdString);
                setSelectedGroup(groupId, false);
            }
        }
    }

    private void getTreeTypes(ClusterFlyweight clusterFlyweight, Set<Integer> typeIds) {
        if (clusterFlyweight.getClusterKey() != null) {
            typeIds.add(clusterFlyweight.getClusterKey().getResourceTypeId());
        }

        for (ClusterFlyweight child : clusterFlyweight.getChildren()) {
            getTreeTypes(child, typeIds);
        }
    }

    class ResourceGroupEnhancedTreeNode extends EnhancedTreeNode {
        private static final String TOOLTIP_KEY = "tooltip";
        private static final String CLUSTER_KEY = "key";
        private static final String RESOURCE_TYPE = "resourceType";

        public ResourceGroupEnhancedTreeNode(String name) {
            super(name);
        }

        public String getTooltip() {
            return getAttribute(TOOLTIP_KEY);
        }

        public void setTooltip(String tooltip) {
            setAttribute(TOOLTIP_KEY, tooltip);
        }

        public ClusterKey getClusterKey() {
            return (ClusterKey) getAttributeAsObject(CLUSTER_KEY);
        }

        public void setClusterKey(ClusterKey key) {
            setAttribute(CLUSTER_KEY, key);
        }

        @Override
        public void setID(String id) {
            super.setID(SeleniumUtility.getSafeId(id));
        }

        @Override
        public void setParentID(String parentID) {
            super.setParentID(SeleniumUtility.getSafeId(parentID));
        }

        public ResourceType getResourceType() {
            return (ResourceType) getAttributeAsObject(RESOURCE_TYPE);
        }

        public void setResourceType(ResourceType rt) {
            setAttribute(RESOURCE_TYPE, rt);
        }

        public boolean isTopNode() {
            return getParentID().equals(FAKE_ROOT_ID);
        }

        public boolean isMixedGroupTopNode() {
            return isTopNode() && (null == getResourceType());
        }

        public boolean isCompatibleGroupTopNode() {
            return isTopNode() && (null != getResourceType()) && (null == getClusterKey());
        }

        public boolean isAutoGroupNode() {
            return !isTopNode() && (null != getResourceType()) && (null == getClusterKey());
        }

        public boolean isAutoClusterNode() {
            return (null != getResourceType()) && (null != getClusterKey()) && !isTopNode();
        }

        public boolean isSubCategoryNode() {
            return (null == getResourceType()) && !isMixedGroupTopNode();
        }

    }

}
