/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * ---------------------------------------------------------------------------
 *
 * Red Hat, Inc. elects to include this software in this distribution under
 * the GPL Version 2 license.
 */
package org.rhq.core.gui.table.renderer;

import com.sun.faces.renderkit.RenderKitUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import java.io.IOException;
import java.util.Map;

/**
 * <B>ButtonRenderer</B> is a class that renders the current value of <code>UICommand<code> as a Button.
 *
 * NOTE: This class is a lightly modified copy of the <code>com.sun.faces.renderkit.html_basic.ButtonRenderer</code>
 *       class from the JSF RI v1.2_14 jsf-impl.jar.
 *
 * @author java.net JSF RI project
 * @author Ian Springer
 */
public abstract class AbstractButtonRenderer extends AbstractRenderer {
    private static final String[] ATTRIBUTES = new String[] {
                            "accesskey",
                            "alt",
                            "dir",
                            "lang",
                            "onblur",
                            "onchange",
                            "onclick",
                            "ondblclick",
                            "onfocus",
                            "onkeydown",
                            "onkeypress",
                            "onkeyup",
                            "onmousedown",
                            "onmousemove",
                            "onmouseout",
                            "onmouseover",
                            "onmouseup",
                            "onselect",
                            "style",
                            "tabindex",
                            "title",
                };

    private final Log log = LogFactory.getLog(this.getClass());

    // ---------------------------------------------------------- Public Methods


    @Override
    public void decode(FacesContext context, UIComponent component) {
        validateParameters(context, component);
        if (wasClicked(context, component) && !isReset(component)) {
            component.queueEvent(new ActionEvent(component));
            if (log.isDebugEnabled()) {
                log.debug("This command resulted in form submission - ActionEvent queued. Finished decoding "
                        + component.getClass().getSimpleName() + " component with id " + component.getId() + ".");
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component)
          throws IOException {
        validateParameters(context, component);

        // Which button type (SUBMIT, RESET, or BUTTON) should we generate?
        String type = getButtonType(component);

        ResponseWriter writer = context.getResponseWriter();
        assert(writer != null);

        String label = "";
        Object value = ((UICommand) component).getValue();
        if (value != null) {
            label = value.toString();
        }
        String imageSrc = (String) component.getAttributes().get("image");
        writer.startElement("input", component);
        writeIdAttributeIfNecessary(context, writer, component);
        String clientId = component.getClientId(context);
        if (imageSrc != null) {
            writer.writeAttribute("type", "image", "type");
            writer.writeURIAttribute("src", src(context, imageSrc), "image");
            writer.writeAttribute("name", clientId, "clientId");
        } else {
            writer.writeAttribute("type", type, "type");
            writer.writeAttribute("name", clientId, "clientId");
            writer.writeAttribute("value", label, "value");
        }

        RenderKitUtils.renderPassThruAttributes(writer,
                                                component,
                                                ATTRIBUTES);
        renderBooleanAttributes(writer, component);

        String styleClass = (String)
              component.getAttributes().get("styleClass");
        if (styleClass != null && styleClass.length() > 0) {
            writer.writeAttribute("class", styleClass, "styleClass");
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
          throws IOException {
        validateParameters(context, component);
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("input");
    }

    protected void renderBooleanAttributes(ResponseWriter writer, UIComponent component) throws IOException {
        RenderKitUtils.renderXHTMLStyleBooleanAttributes(writer, component);
    }

    // --------------------------------------------------------- Private Methods


    /**
     * @param context the <code>FacesContext</code> for the current request
     * @param imageURI the base URI of the image to use for the button
     * @return the encoded result for the base imageURI
     */
    private static String src(FacesContext context, String imageURI) {

        if (imageURI == null) {
            return "";
        }

        String u = context.getApplication().getViewHandler()
              .getResourceURL(context, imageURI);
        return (context.getExternalContext().encodeResourceURL(u));
    }


    /**
     * <p>Determine if this component was activated on the client side.</p>
     *
     * @param context the <code>FacesContext</code> for the current request
     * @param component the component of interest
     * @return <code>true</code> if this component was in fact activated,
     *  otherwise <code>false</code>
     */
    private static boolean wasClicked(FacesContext context,
                                      UIComponent component) {
        // Was our command the one that caused this submission?
        // we don' have to worry about getting the value from request parameter
        // because we just need to know if this command caused the submission. We
        // can get the command name by calling currentValue. This way we can
        // get around the IE bug.
        String clientId = component.getClientId(context);
        Map<String, String> requestParameterMap = context.getExternalContext()
              .getRequestParameterMap();
        if (requestParameterMap.get(clientId) == null) {
            StringBuilder builder = new StringBuilder(clientId);
            String xValue = builder.append(".x").toString();
            builder.setLength(clientId.length());
            String yValue = builder.append(".y").toString();
            return (requestParameterMap.get(xValue) != null
                    && requestParameterMap.get(yValue) != null);
        }
        return true;
    }

    /**
     * @param component the component of interest
     * @return <code>true</code> if the button represents a <code>reset</code>
     *  button, otherwise <code>false</code>
     */
    private static boolean isReset(UIComponent component) {
        return ("reset".equals(component.getAttributes().get("type")));
    }

    /**
     * <p>If the component's type attribute is null or not equal
     * to <code>reset</code> or <code>submit</code>, default to
     * <code>submit</code>.
     * @param component the component of interest
     * @return the type for this button
     */
    private static String getButtonType(UIComponent component) {

        String type = (String) component.getAttributes().get("type");
        if (type == null || (!"reset".equals(type) && !"submit".equals(type))) {
            type = "submit";
            // This is needed in the decode method
            component.getAttributes().put("type", type);
        }
        return type;
    }
}
