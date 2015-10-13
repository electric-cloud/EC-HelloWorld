/**
 *  Copyright 2015 Electric Cloud, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ecplugins.helloworld.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import ecinternal.client.DialogClickHandler;
import ecinternal.client.ListBase;


import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.requests.CgiRequestProxy;
import com.electriccloud.commander.client.requests.RunProcedureRequest;
import com.electriccloud.commander.client.responses.DefaultRunProcedureResponseCallback;
import com.electriccloud.commander.client.responses.RunProcedureResponse;
import com.electriccloud.commander.gwt.client.ui.ListTable;
import com.electriccloud.commander.gwt.client.ui.SimpleErrorBox;

import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;

import static ecinternal.client.InternalComponentBaseFactory.getPluginName;



import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createPageUrl;
import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createRedirectUrl;

/**
 * EC-HelloWorld Configuration List.
 */
public class ConfigurationList
    extends ListBase
{

    //~ Instance fields --------------------------------------------------------

    private HelloWorldConfigList m_configList;

    //~ Constructors -----------------------------------------------------------

    public ConfigurationList()
    {
        super("ecgc", "HelloWorld Configurations", "All Configurations");
        m_configList = new HelloWorldConfigList();
    }

    //~ Methods ----------------------------------------------------------------

    @Override protected Anchor constructCreateLink()
    {
        CommanderUrlBuilder urlBuilder = createPageUrl(getPluginName(),
                "newConfiguration");

        urlBuilder.setParameter("redirectTo",
            createRedirectUrl().buildString());

        return new Anchor("Create Configuration", urlBuilder.buildString());
    }

    @Override protected void load()
    {
        setStatus("Loading...");

        HelloWorldConfigListLoader loader = new HelloWorldConfigListLoader(m_configList, this,
                new ChainedCallback() {
                    @Override public void onComplete()
                    {
                        loadList();
                    }
                });

        loader.load();
    }

    private void deleteConfiguration(String configName)
    {
        setStatus("Deleting...");
        clearErrorMessages();

        // Build runProcedure request
        RunProcedureRequest request = getRequestFactory()
                .createRunProcedureRequest();

        request.setProjectName("/plugins/EC-HelloWorld/project");
        request.setProcedureName("DeleteConfiguration");
        request.addActualParameter("config", configName);
        request.setCallback(new DefaultRunProcedureResponseCallback(this) {
                @Override public void handleResponse(
                        RunProcedureResponse response)
                {

                    if (getLog().isDebugEnabled()) {
                        getLog().debug(
                            "Commander runProcedure request returned jobId: "
                                + response.getJobId());
                    }

                    waitForJob(response.getJobId().toString());
                }
            });

        if (getLog().isDebugEnabled()) {
            getLog().debug("Issuing Commander request: " + request);
        }

        doRequest(request);
    }

    private void loadList()
    {
        ListTable listTable = getListTable();

        if (!m_configList.isEmpty()) {
            listTable.addHeaderRow(true, "Configuration Name", "Level of Formality");
        }

        for (String configName : m_configList.getConfigNames()) {

            // Config name
            Label configNameLabel = new Label(configName);

            // configuration parameters
            String formalLevel      = m_configList.getFormalLevel(configName);
            Label  formalLevelLabel = new Label(formalLevel);

            // "Edit" link
            CommanderUrlBuilder urlBuilder = createPageUrl(getPluginName(),
                    "editConfiguration");

            urlBuilder.setParameter("configName", configName);
            urlBuilder.setParameter("redirectTo",
                createRedirectUrl().buildString());

            Anchor editConfigLink = new Anchor("Edit",
                    urlBuilder.buildString());

            // "Delete" link
            Anchor             deleteConfigLink = new Anchor("Delete");
            DialogClickHandler dch              = new DialogClickHandler(
                    new DeleteConfirmationDialog(configName,
                        "Are you sure you want to delete the HelloWorld configuration '"
                            + configName + "'?") {
                        @Override protected void doDelete()
                        {
                            deleteConfiguration(m_objectId);
                        }
                    });

            deleteConfigLink.addClickHandler(dch);

            // Add the row
            editConfigLink.getElement()
                          .setId(getIdPrefix() + "-edit");
            deleteConfigLink.getElement()
                            .setId(getIdPrefix() + "-delete");

            Widget actions = this.getUIFactory().constructActionList(editConfigLink,
                    deleteConfigLink);

            listTable.addRow(configNameLabel, formalLevelLabel, actions);
        }

        clearStatus();
    }

    private void waitForJob(final String jobId)
    {
        CgiRequestProxy     cgiRequestProxy = new CgiRequestProxy(
                getPluginName(), "helloworldMonitor.cgi");
        Map<String, String> cgiParams       = new HashMap<String, String>();

        cgiParams.put("jobId", jobId);

        // Pass debug flag to CGI, which will use it to determine whether to
        // clean up a successful job
        if ("1".equals(getGetParameter("debug"))) {
            cgiParams.put("debug", "1");
        }

        try {
            cgiRequestProxy.issueGetRequest(cgiParams, new RequestCallback() {
                    @Override public void onError(
                            Request   request,
                            Throwable exception)
                    {
                        addErrorMessage("CGI request failed:: ", exception);
                    }

                    @Override public void onResponseReceived(
                            Request  request,
                            Response response)
                    {
                        String responseString = response.getText();

                        if (getLog().isDebugEnabled()) {
                            getLog().debug(
                                "CGI response received: " + responseString);
                        }

                        if (responseString.startsWith("Success")) {

                            // We're done!
                            Location.reload();
                        }
                        else {
                            SimpleErrorBox      error      = getUIFactory()
                                    .createSimpleErrorBox(
                                        "Error occurred during configuration deletion: "
                                        + responseString);
                            CommanderUrlBuilder urlBuilder = CommanderUrlBuilder
                                    .createUrl("jobDetails.php")
                                    .setParameter("jobId", jobId);

                            error.add(
                                new Anchor("(See job for details)",
                                    urlBuilder.buildString()));
                            addErrorMessage(error);
                        }
                    }
                });
        }
        catch (RequestException e) {
            addErrorMessage("CGI request failed:: ", e);
        }
    }
}
