/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.tagging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.MouseOutEvent;
import com.smartgwt.client.widgets.events.MouseOutHandler;
import com.smartgwt.client.widgets.events.MouseOverEvent;
import com.smartgwt.client.widgets.events.MouseOverHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;

import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDialog;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLayout;

/**
 * A reusable component that shows a set of tags and, if not read only, allows the user
 * to delete existing tags or add new tags.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class TagEditorView extends LocatableLayout {

    private LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();
    private boolean readOnly;
    private TagsChangedCallback callback;
    private HTMLFlow tagTitleLabel;
    private ArrayList<LocatableHLayout> tagLayouts;
    private Img addImg;
    private TagInputDialog tagInputDialog;

    public TagEditorView(String locatorId, Set<Tag> tags, boolean readOnly, TagsChangedCallback callback) {
        this(locatorId, tags, readOnly, callback, false);
    }

    public TagEditorView(String locatorId, Set<Tag> tags, boolean readOnly, TagsChangedCallback callback,
        boolean vertical) {

        super(locatorId);

        setVertical(vertical);
        setAutoWidth();
        if (!vertical) {
            setMembersMargin(8);
        }

        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.readOnly = readOnly;
        this.callback = callback;

        tagTitleLabel = new HTMLFlow("<b>" + MSG.view_tags_tags() + ":</b>");
        tagTitleLabel.setAutoWidth();

        if (!this.readOnly) {
            tagInputDialog = new TagInputDialog(extendLocatorId("tagInputDialog"));

            addImg = new LocatableImg(extendLocatorId("addImg"), "[skin]/images/actions/add.png", 16, 16);
            addImg.setTooltip(MSG.view_tags_tooltip_2());
            addImg.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    showTagInput();
                }
            });
        }
    }

    public LinkedHashSet<Tag> getTags() {
        return tags;
    }

    public void setTags(LinkedHashSet<Tag> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
        setup();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        setup();
    }

    private void setup() {
        // remove old members
        removeMember(tagTitleLabel);
        if (tagLayouts != null) {
            for (LocatableHLayout tagLayout : tagLayouts) {
                removeMember(tagLayout);
                tagLayout.destroy();
            }
        }
        if (addImg != null) {
            removeMember(addImg);
        }

        // add new members
        addMember(tagTitleLabel);
        tagLayouts = createTagLayouts();
        for (LocatableHLayout tagLayout : tagLayouts) {
            addMember(tagLayout);
        }
        if (!readOnly) {
            addMember(addImg);
            tagInputDialog.place(addImg);
        }

        markForRedraw();
    }

    private ArrayList<LocatableHLayout> createTagLayouts() {
        ArrayList<LocatableHLayout> tagLayouts = new ArrayList<LocatableHLayout>(tags.size());

        for (final Tag tag : tags) {
            LocatableHLayout tagLayout = new LocatableHLayout(extendLocatorId(tag.getName()));
            tagLayout.setHeight(18);
            tagLayout.setHeight(16);

            HTMLFlow tagString = new HTMLFlow("<a href=\"" + LinkManager.getTagLink(tag.toString()) + "\">"
                + tag.toString() + "</a>");
            tagString.setAutoWidth();
            tagLayout.addMember(tagString);

            if (!readOnly) {
                final LayoutSpacer spacer = new LayoutSpacer();
                spacer.setHeight(16);
                spacer.setWidth(16);

                final Img remove = new LocatableImg(tagLayout.extendLocatorId("Remove"),
                    "[skin]/images/actions/remove.png", 16, 16);
                remove.setTooltip(MSG.view_tags_tooltip_1());
                remove.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        tags.remove(tag);
                        save();
                    }
                });

                tagLayout.addMember(remove);
                tagLayout.addMember(spacer);
                remove.hide();

                tagLayout.addMouseOverHandler(new MouseOverHandler() {
                    public void onMouseOver(MouseOverEvent mouseOverEvent) {
                        remove.show();
                        spacer.hide();
                    }
                });
                tagLayout.addMouseOutHandler(new MouseOutHandler() {
                    public void onMouseOut(MouseOutEvent mouseOutEvent) {
                        spacer.show();
                        remove.hide();
                    }
                });
            }

            tagLayouts.add(tagLayout);
        }

        return tagLayouts;
    }

    private void showTagInput() {
        TagCriteria criteria = new TagCriteria();
        criteria.addSortNamespace(PageOrdering.ASC);
        criteria.addSortSemantic(PageOrdering.ASC);
        criteria.addSortName(PageOrdering.ASC);
        GWTServiceLookup.getTagService().findTagsByCriteria(criteria, new AsyncCallback<PageList<Tag>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_tags_error_1(), caught);
            }

            public void onSuccess(PageList<Tag> result) {
                String[] values = new String[result.size()];
                int i = 0;
                for (Tag tag : result) {
                    values[i++] = tag.toString();
                }
                tagInputDialog.setTagSuggestions(values);
            }
        });

        tagInputDialog.show();
        tagInputDialog.place(addImg);
        markForRedraw();
    }

    private void save() {
        this.callback.tagsChanged(tags);
        TagEditorView.this.setup();
    }

    private class TagInputDialog extends LocatableDialog {
        private ComboBoxItem tagInputItem;

        public TagInputDialog(String locatorId) {
            super(locatorId);

            setIsModal(true);
            setShowHeader(false);
            setShowEdges(false);
            setEdgeSize(10);
            setWidth(200);
            setHeight(30);
            setShowToolbar(false);
            setDismissOnEscape(true);
            setDismissOnOutsideClick(true);
            Map<String, Integer> bodyDefaults = new HashMap<String, Integer>(2);
            bodyDefaults.put("layoutLeftMargin", 5);
            bodyDefaults.put("membersMargin", 10);
            setBodyDefaults(bodyDefaults);

            final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("tagInputForm"));
            addItem(form);

            tagInputItem = new ComboBoxItem("tag");
            tagInputItem.setShowTitle(false);
            tagInputItem.setHideEmptyPickList(true);
            tagInputItem.setValueField("tag");
            tagInputItem.setDisplayField("tag");
            tagInputItem.setType("comboBox");
            tagInputItem.setTextMatchStyle(TextMatchStyle.SUBSTRING);
            tagInputItem.setTooltip(MSG.view_tags_tooltip_3());
            tagInputItem.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    if ((event.getCharacterValue() != null) && (event.getCharacterValue() == KeyCodes.KEY_ENTER)) {
                        //String tag = form.getValueAsString("tag");
                        String tag = tagInputItem.getEnteredValue();
                        if (tag != null) {
                            Tag newTag = new Tag(tag);
                            tags.add(newTag);
                            TagEditorView.this.save();
                            TagInputDialog.this.hide();
                        }
                    }
                }
            });
            form.setFields(tagInputItem);
        }

        @Override
        public void show() {
            super.show();
            tagInputItem.clearValue();
            tagInputItem.focusInItem();
        }

        public void setTagSuggestions(String[] suggestions) {
            tagInputItem.setValueMap(suggestions);
        }

        public void place(Canvas canvas) {
            // move this object over top the given canvas
            moveTo(canvas.getAbsoluteLeft() - 8, canvas.getAbsoluteTop() - 4);
        }
    }
}