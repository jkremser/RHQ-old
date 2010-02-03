package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

public class RhnJaxbTransport extends CustomReqPropTransport {

    protected String jaxbDomain;

    private final Log log = LogFactory.getLog(RhnJaxbTransport.class);

    public RhnJaxbTransport(XmlRpcClient pClient) {
        super(pClient);
    }

    public void setJaxbDomain(String domain) {
        jaxbDomain = domain;
    }

    public String getJaxbDomain() {
        return jaxbDomain;
    }

    protected boolean isJaxbMessage(BufferedInputStream bufIn) throws IOException {
        // Requiring BufferedInputStream so it will support the mark/reset methods
        // We want to read the beginning of the data stream to determine if it's a <rhn-satellite> XML message
        // true = rhn-satellite xml message, false = some other form of xml
        // before we return, we want to reset the BufferedInputStream so it's pointing back to where it was when
        // we were called.

        int readlimit = 1 * 1024;
        byte[] buffer = new byte[readlimit];
        bufIn.mark(readlimit);
        bufIn.read(buffer);
        bufIn.reset();
        String temp = new String(buffer);
        if (temp.contains("<rhn-satellite")) {
            return true;
        }
        return false;
    }

    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
        /**
         * Point of this method is to not require the traditional "methodResponse" xml wrapping
         * around the response.  RHN just returns the pure XML data.
         *
         * For error conditions, RHN defaults back to using "methodResponse".
         * We need to check what the top element is.  If it's "rhn-satellite" do JAXB parsing,
         * if it's "methodResponse" do traditional XMLRPC parsing.
         *
         * */
        BufferedInputStream bufIn = new BufferedInputStream(pStream);
        try {
            if (isJaxbMessage(bufIn) == false) {
                log.info("Message is not a JAXB element");
                return super.readResponse(pConfig, bufIn);
            }
            if (getDumpMessageToFile()) {
                File tempFile = cacheResponseToFile(bufIn);
                bufIn.close();
                try {
                    bufIn = new BufferedInputStream(new FileInputStream(tempFile));
                } catch (FileNotFoundException e) {
                    log.error(e);
                    throw new XmlRpcException(e.getMessage());
                }
            }
            JAXBContext jc = JAXBContext.newInstance(jaxbDomain);
            Unmarshaller u = jc.createUnmarshaller();
            return u.unmarshal(bufIn);
        } catch (JAXBException e) {
            log.error(e);
            throw new XmlRpcException(e.getMessage());
        } catch (IOException e) {
            log.error(e);
            throw new XmlRpcException(e.getMessage());
        } finally {
            try {
                if (bufIn != null) {
                    bufIn.close();
                }
            } catch (Exception e) {
                ; //ignore exception from close
            }
        }
    }

}
