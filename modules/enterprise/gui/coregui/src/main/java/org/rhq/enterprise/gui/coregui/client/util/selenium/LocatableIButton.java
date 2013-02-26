package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.IButton;

/**
 * Wrapper for com.smartgwt.client.widgets.IButton that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableIButton extends IButton implements Locatable {

    private String locatorId;

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableIButton(String locatorId) {
        super();
        init(locatorId);
    }

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param title
     */
    public LocatableIButton(String locatorId, String title) {
        super(title);
        init(locatorId);
    }

    private void init(String locatorId) {
        this.locatorId = locatorId;
        SeleniumUtility.setID(this, locatorId);
        String title = getTitle();
        if (title != null && title.length() > 15) {
            setAutoFit(true);
        }
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "_" + extension;
    }
}
