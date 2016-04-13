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

package de.bayern.gdi.experimental.gui;

import de.bayern.gdi.experimental.ServiceSetting;
import de.bayern.gdi.experimental.services.WebService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;


/**
 * @author Jochen Saalfeld (jochen@intevation.de)
 */
public class DataBean extends Observable {

    private Stage primaryStage = null;
    private Map<String , String> namePwMap = null;
    private ServiceSetting serviceSetting = null;
    private Map<String, String> services;
    private Map<String, String> catalogues;
    private WebService webService;

    /**
     * Constructor.
     */
    public DataBean(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.namePwMap = new HashMap<>();
        this.serviceSetting = new ServiceSetting();
        this.services = this.serviceSetting.getServices();
        this.catalogues = this.serviceSetting.getCatalogues();
        this.webService = null;
    }

    /**
     * returns the Name Map.
     * @return the Name Map
     */
    public Map<String, String> getNamePwMap() {
        return namePwMap;
    }

    /**
     * Builds a Observable List from the services Map.
     * @return List build from services Map
     */
    public ObservableList<String> getServicesAsList() {
        ObservableList<String> serviceNames =
                FXCollections.observableArrayList();
        Iterator it = this.services.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            serviceNames.add((String) pair.getKey());
        }
        return serviceNames;
    }

    /**
     * Returns the Service URL for a given Service Name.
     * @param serviceName name of a Service
     * @return the url of the service
     */
    public String getServiceURL(String serviceName) {
        String returnStr = null;
        if (this.services.containsKey(serviceName)) {
            returnStr = this.services.get(serviceName);
        }
        return returnStr;
    }

    /**
     * returns the current stage.
     * @return the stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void printStringMap(Map<String, String> map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }

    /**
     * gets the Webservice.
     * @return webservice
     */
    public WebService getWebService() {
        return webService;
    }

    /**
     * sets the webservice.
     * @param webService webservice
     */
    public void setWebService(WebService webService) {
        this.webService = webService;
    }

    /**
     * returns true if webservice is set.
     * @return true if webservice ist set; false if not set
     */
    public boolean isWebServiceSet() {
        if (this.webService == null) {
            return false;
        }
        return true;

    }

}
