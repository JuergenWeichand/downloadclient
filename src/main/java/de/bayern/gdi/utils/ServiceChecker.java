/*
 * DownloadClient Geodateninfrastruktur Bayern
 *
 * (c) 2016 GSt. GDI-BY (gdi.bayern.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bayern.gdi.utils;

import de.bayern.gdi.services.ServiceType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathConstants;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */
public class ServiceChecker {

    private static final Logger log
            = Logger.getLogger(ServiceChecker.class.getName());

    private ServiceChecker() {
    }

    /**
     * checks the service type.
     *
     * @param serviceURL the service url
     * @return the type of service; null if failed
     */
    public static ServiceType checkService(
            URL serviceURL
    ) {
        return checkService(serviceURL, null, null);
    }

    /**
     * checks the service type.
     *
     * @param serviceURL the service url
     * @param user       The optional user name.
     * @param password   The optional password.
     * @return the type of service; null if failed
     */
    public static ServiceType checkService(
            String serviceURL,
            String user,
            String password
    ) {
        try {
            return checkService(new URL(serviceURL), user, password);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    /**
     * checks the service type.
     *
     * @param serviceURL the service url
     * @param user       The optional user name.
     * @param password   The optional password.
     * @return the type of service; null if failed
     */
    public static ServiceType checkService(
            URL serviceURL,
            String user,
            String password
    ) {
        Document doc = null;
        try {
            if (isReachable(serviceURL)) {
                if (simpleRestricted(serviceURL)) {
                    if (user != null && password != null) {
                        doc = XML.getDocument(
                                serviceURL,
                                user, password);
                    } else {
                        return null;
                    }
                } else {
                    doc = XML.getDocument(
                            serviceURL,
                            null, null);
                }
            }
            if (doc == null) {
                return null;
            }
        } catch (URISyntaxException
                | IOException e) {
            log.log(Level.SEVERE, "Could not get Document of URL: "
                    + serviceURL.toString(), e);
        }


        //It seems that there is more than one implementation of this
        //stuff...
        final String wfs = "http://www.opengis.net/wfs/2.0";
        NodeList nl = doc.getElementsByTagNameNS(wfs, "WFS_Capabilities");
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("WFS_Capabilities");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("WFS_CAPABILITIES");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("wfs_capabilities");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("wfs:wfs_capabilities");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("WFS:wfs_capabilities");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("wfs:WFS_CAPABILITIES");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("WFS:WFS_CAPABILITIES");
        }
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName("wfs:WFS_Capabilities");
        }
        if (nl.getLength() != 0) {
            NamedNodeMap nnm = nl.item(0).getAttributes();
            if (nnm.getNamedItem("version") != null) {
                switch (nnm.getNamedItem("version").getNodeValue()) {
                    case "1.0.0":
                    case "1.1.0":
                        return ServiceType.WFSOne;
                    case "2.0.0":
                        return ServiceType.WFSTwo;
                    default:
                        return null;
                }
            } else {
                return null;
            }
        }
        nl = doc.getElementsByTagName("feed");
        if (nl.getLength() != 0) {
            Node n = nl.item(0);
            NamedNodeMap nnm = n.getAttributes();
            String wfsVersion = nnm.getNamedItem("xmlns").getNodeValue();
            if (wfsVersion.toLowerCase().endsWith("atom")) {
                return ServiceType.Atom;
            }
        }

        return null;
    }

    /**
     * Checks if a Service is restricted.
     *
     * @param url the URL of the service
     * @return true if restriced; false if not
     */
    public static boolean isRestricted(URL url) {
        if (checkService(url) == ServiceType.Atom) {
            if (simpleRestricted(url)) {
                return true;
            }
            Document mainXML = null;
            try {
                mainXML = XML.getDocument(url, false);
            } catch (URISyntaxException
                    | IOException e) {
                log.log(Level.SEVERE, "Could not get Document of URL: "
                        + url.toString(), e);
                return true;
            }
            String describedByExpr =
                    "/feed/entry/link[@rel='alternate']/@href[1]";
            String describedBy = (String) XML.xpath(mainXML,
                    describedByExpr,
                    XPathConstants.STRING);
            if (describedBy == null) {
                return false;
            }
            try {
                URL entryURL = HTTP.buildAbsoluteURL(url, describedBy);
                if (simpleRestricted(url)) {
                    return true;
                }
                Document entryDoc = null;
                try {
                     entryDoc = XML.getDocument(entryURL, false);
                } catch (URISyntaxException
                    | IOException e) {
                    log.log(Level.SEVERE, "Could not get Document of URL: "
                        + entryURL.toString(), e);
                    return true;
                }
                String downloadURLExpr =
                        "/feed/entry/link/@href[1]";
                String downloadURLStr = (String) XML.xpath(entryDoc,
                        downloadURLExpr,
                        XPathConstants.STRING);
                if (describedBy == null) {
                    return false;
                }
                URL downloadURL = HTTP.buildAbsoluteURL(url, downloadURLStr);
                return simpleRestricted(downloadURL);
            } catch (URISyntaxException | MalformedURLException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            return simpleRestricted(url);
        }
        return false;
    }

    /**
     * Checks if a URL is restricted.
     *
     * @param url the url
     * @return true if restricted; false if not
     */
    public static boolean simpleRestricted(URL url) {
        try {
            int ret = tryHead(url);
            if (ret != HttpStatus.SC_OK) {
                return true;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return false;
    }

    /**
     * trys to make a head request against url.
     * @param url the url
     * @return HTTP Return code
     * @throws IOException if something goes wrong
     */
    public static int tryHead(URL url)
        throws IOException {
        try {
            CloseableHttpClient httpCl = HTTP.getClient(url, null, null);
            HttpHead getRequest = HTTP.getHeadRequest(url);
            CloseableHttpResponse execute = httpCl.execute(getRequest);
            StatusLine statusLine = execute.getStatusLine();
            return statusLine.getStatusCode();
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Checks if a URL is reachable.
     *
     * @param url url
     * @return true if reachable; false if not
     */
    public static boolean isReachable(String url) {
        try {
            return isReachable(new URL(url));
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a URL is reachable.
     *
     * @param url url
     * @return true if reachable; false if not
     */
    public static boolean isReachable(URL url) {
        try {
            int retcode = tryHead(url);
            // Removing statusLine.getStatusCode() == HttpStatus.SC_FORBIDDEN
            // because special MS "Standards"
            // (https://en.wikipedia.org/wiki/HTTP_403)
            if (retcode == HttpStatus.SC_OK
                || retcode == HttpStatus.SC_UNAUTHORIZED) {
                return true;
            }
            return false;
        } catch (IOException e) {
            //log.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }
    }
}
