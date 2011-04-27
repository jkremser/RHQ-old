package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.layout.HStack;

/**
 * Wrapper for a SmartGWT {@link HStack} that sets the ID for use with Selenium scLocators.
 *
 * @author Jay Shaughnessy
 */
public class LocatableHStack extends HStack implements Locatable {

    private String locatorId;

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableHStack(String locatorId) {
        super();
        init(locatorId);
    }

    /**
     * <pre>
     * ID Format: "simpleClassname_locatorId"
     * </pre>
     * @param locatorId not null or empty.
     * @param membersMargin
     */
    public LocatableHStack(String locatorId, int membersMargin) {
        super(membersMargin);
        init(locatorId);
    }

    private void init(String locatorId) {
        this.locatorId = locatorId;
        SeleniumUtility.setID(this, locatorId);
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "_" + extension;
    }

    public void destroyMembers() {
        SeleniumUtility.destroyMembers(this);
    }

    @Override
    public void destroy() {
        destroyMembers();
        super.destroy();
    }

}
