package org.rhq.gui.content;

import java.security.cert.X509Extension;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContentFilter {

    private final Log log = LogFactory.getLog(ContentFilter.class);

    static final String CERTS = "javax.servlet.request.X509Certificate";

    void filter(HttpServletRequest request, int repoId) throws EntitlementException {
        X509Extension[] certificates = (X509Extension[]) request.getAttribute(CERTS);
        if (certificates == null) {
            throw new EntitlementException("No X509 certificate found.");
        }
        String oid = objectIdentifier(repoId);
        for (X509Extension x509 : certificates) {
            byte[] bytes = x509.getExtensionValue(oid);
            if (bytes != null) {
                return;
            }
        }
        throw new EntitlementException("oid (" + oid + ") not found");
    }

    private String objectIdentifier(int repoId) {
        String idstr = String.valueOf(repoId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < idstr.length(); i++) {
            if (i > 0)
                sb.append('.');
            sb.append(idstr.charAt(i));
        }
        return sb.toString();
    }
}
