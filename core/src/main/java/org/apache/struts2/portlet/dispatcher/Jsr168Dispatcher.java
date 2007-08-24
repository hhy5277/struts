/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2.portlet.dispatcher;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionProxyFactory;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.util.FileManager;
import com.opensymphony.xwork2.util.LocalizedTextUtil;
import com.opensymphony.xwork2.util.TextUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.StrutsException;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.dispatcher.ApplicationMap;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.RequestMap;
import org.apache.struts2.dispatcher.SessionMap;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.portlet.PortletActionConstants;
import org.apache.struts2.portlet.PortletApplicationMap;
import org.apache.struts2.portlet.PortletRequestMap;
import org.apache.struts2.portlet.PortletSessionMap;
import org.apache.struts2.portlet.context.PortletActionContext;
import org.apache.struts2.portlet.servlet.PortletServletContext;
import org.apache.struts2.portlet.servlet.PortletServletRequest;
import org.apache.struts2.portlet.servlet.PortletServletResponse;
import org.apache.struts2.util.AttributeMap;

import javax.portlet.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <!-- START SNIPPET: javadoc -->
 * <p>
 * Struts JSR-168 portlet dispatcher. Similar to the WW2 Servlet dispatcher,
 * but adjusted to a portal environment. The portlet is configured through the <tt>portlet.xml</tt>
 * descriptor. Examples and descriptions follow below:
 * </p>
 * <!-- END SNIPPET: javadoc -->
 *
 * @author Nils-Helge Garli
 * @author Rainer Hermanns
 *
 * <p><b>Init parameters</b></p>
 * <!-- START SNIPPET: params -->
 * <table class="confluenceTable">
 * <tr>
 *  <th class="confluenceTh">Name</th>
 * <th class="confluenceTh">Description</th>
 * <th class="confluenceTh">Default value</th>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">portletNamespace</td><td class="confluenceTd">The namespace for the portlet in the xwork configuration. This
 *      namespace is prepended to all action lookups, and makes it possible to host multiple
 *      portlets in the same portlet application. If this parameter is set, the complete namespace
 *      will be <tt>/portletNamespace/modeNamespace/actionName</tt></td><td class="confluenceTd">The default namespace</td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">viewNamespace</td><td class="confluenceTd">Base namespace in the xwork configuration for the <tt>view</tt> portlet
 *      mode</td><td class="confluenceTd">The default namespace</td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">editNamespace</td><td class="confluenceTd">Base namespace in the xwork configuration for the <tt>edit</tt> portlet
 *      mode</td><td class="confluenceTd">The default namespace</td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">helpNamespace</td><td class="confluenceTd">Base namespace in the xwork configuration for the <tt>help</tt> portlet
 *      mode</td><td class="confluenceTd">The default namespace</td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">defaultViewAction</td><td class="confluenceTd">Default action to invoke in the <tt>view</tt> portlet mode if no action is
 *      specified</td><td class="confluenceTd"><tt>default</tt></td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">defaultEditAction</td><td class="confluenceTd">Default action to invoke in the <tt>edit</tt> portlet mode if no action is
 *      specified</td><td class="confluenceTd"><tt>default</tt></td>
 * </tr>
 * <tr>
 *  <td class="confluenceTd">defaultHelpAction</td><td class="confluenceTd">Default action to invoke in the <tt>help</tt> portlet mode if no action is
 *      specified</td><td class="confluenceTd"><tt>default</tt></td>
 * </tr>
 * </table>
 * <!-- END SNIPPET: params -->
 * <p><b>Example:</b></p>
 * <pre>
 * <!-- START SNIPPET: example -->
 *
 * &lt;init-param&gt;
 *     &lt;!-- The view mode namespace. Maps to a namespace in the xwork config file --&gt;
 *     &lt;name&gt;viewNamespace&lt;/name&gt;
 *     &lt;value&gt;/view&lt;/value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *    &lt;!-- The default action to invoke in view mode --&gt;
 *    &lt;name&gt;defaultViewAction&lt;/name&gt;
 *    &lt;value&gt;index&lt;/value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;!-- The view mode namespace. Maps to a namespace in the xwork config file --&gt;
 *     &lt;name&gt;editNamespace&lt;/name&gt;
 *     &lt;value&gt;/edit&lt;/value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;!-- The default action to invoke in view mode --&gt;
 *     &lt;name&gt;defaultEditAction&lt;/name&gt;
 *     &lt;value&gt;index&lt;/value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;!-- The view mode namespace. Maps to a namespace in the xwork config file --&gt;
 *     &lt;name&gt;helpNamespace&lt;/name&gt;
 *     &lt;value&gt;/help&lt;/value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;!-- The default action to invoke in view mode --&gt;
 *     &lt;name&gt;defaultHelpAction&lt;/name&gt;
 *     &lt;value&gt;index&lt;/value&gt;
 * &lt;/init-param&gt;
 *
 * <!-- END SNIPPET: example -->
 * </pre>
 */
public class Jsr168Dispatcher extends GenericPortlet implements StrutsStatics,
        PortletActionConstants {

    private static final Log LOG = LogFactory.getLog(Jsr168Dispatcher.class);

    private ActionProxyFactory factory = null;

    private Map<PortletMode,String> modeMap = new HashMap<PortletMode,String>(3);

    private Map<PortletMode,ActionMapping> actionMap = new HashMap<PortletMode,ActionMapping>(3);

    private String portletNamespace = null;

    private Dispatcher dispatcherUtils;
    
    private ActionMapper actionMapper;

    /**
     * Initialize the portlet with the init parameters from <tt>portlet.xml</tt>
     */
    public void init(PortletConfig cfg) throws PortletException {
        super.init(cfg);
        LOG.debug("Initializing portlet " + getPortletName());
        
        Map<String,String> params = new HashMap<String,String>();
        for (Enumeration e = cfg.getInitParameterNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            String value = cfg.getInitParameter(name);
            params.put(name, value);
        }

        // TODO: CHECK IF NEEDED (RG Portlet Refactoring Backports)
        Dispatcher.setPortletSupportActive(true);
        dispatcherUtils = new Dispatcher(new PortletServletContext(cfg.getPortletContext()), params);
        dispatcherUtils.init();
        
        // For testability
        if (factory == null) {
            factory = dispatcherUtils.getConfigurationManager().getConfiguration().getContainer().getInstance(ActionProxyFactory.class);
        }
        portletNamespace = cfg.getInitParameter("portletNamespace");
        LOG.debug("PortletNamespace: " + portletNamespace);
        parseModeConfig(cfg, PortletMode.VIEW, "viewNamespace",
                "defaultViewAction");
        parseModeConfig(cfg, PortletMode.EDIT, "editNamespace",
                "defaultEditAction");
        parseModeConfig(cfg, PortletMode.HELP, "helpNamespace",
                "defaultHelpAction");
        parseModeConfig(cfg, new PortletMode("config"), "configNamespace",
                "defaultConfigAction");
        parseModeConfig(cfg, new PortletMode("about"), "aboutNamespace",
                "defaultAboutAction");
        parseModeConfig(cfg, new PortletMode("print"), "printNamespace",
                "defaultPrintAction");
        parseModeConfig(cfg, new PortletMode("preview"), "previewNamespace",
                "defaultPreviewAction");
        parseModeConfig(cfg, new PortletMode("edit_defaults"),
                "editDefaultsNamespace", "defaultEditDefaultsAction");
        if (!TextUtils.stringSet(portletNamespace)) {
            portletNamespace = "";
        }
        LocalizedTextUtil
                .addDefaultResourceBundle("org/apache/struts2/struts-messages");

        Container container = dispatcherUtils.getContainer();
        //check for configuration reloading
        if ("true".equalsIgnoreCase(container.getInstance(String.class, StrutsConstants.STRUTS_CONFIGURATION_XML_RELOAD))) {
            FileManager.setReloadingConfigs(true);
        }
        
        actionMapper = container.getInstance(ActionMapper.class);
    }

    /**
     * Parse the mode to namespace mappings configured in portlet.xml
     * @param portletConfig The PortletConfig
     * @param portletMode The PortletMode
     * @param nameSpaceParam Name of the init parameter where the namespace for the mode
     * is configured.
     * @param defaultActionParam Name of the init parameter where the default action to
     * execute for the mode is configured.
     */
    private void parseModeConfig(PortletConfig portletConfig,
            PortletMode portletMode, String nameSpaceParam,
            String defaultActionParam) {
        String namespace = portletConfig.getInitParameter(nameSpaceParam);
        if (!TextUtils.stringSet(namespace)) {
            namespace = "";
        }
        modeMap.put(portletMode, namespace);
        String defaultAction = portletConfig
                .getInitParameter(defaultActionParam);
        if (!TextUtils.stringSet(defaultAction)) {
            defaultAction = DEFAULT_ACTION_NAME;
        }
        StringBuffer fullPath = new StringBuffer();
        if (TextUtils.stringSet(portletNamespace)) {
            fullPath.append(portletNamespace);
        }
        if (TextUtils.stringSet(namespace)) {
            fullPath.append(namespace).append("/");
        } else {
            fullPath.append("/");
        }
        fullPath.append(defaultAction);
        ActionMapping mapping = new ActionMapping();
        mapping.setName(getActionName(fullPath.toString()));
        mapping.setNamespace(getNamespace(fullPath.toString()));
        actionMap.put(portletMode, mapping);
    }

    /**
     * Service an action from the <tt>event</tt> phase.
     *
     * @see javax.portlet.Portlet#processAction(javax.portlet.ActionRequest,
     *      javax.portlet.ActionResponse)
     */
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        LOG.debug("Entering processAction");
        resetActionContext();
        try {
            serviceAction(request, response, getActionMapping(request),
                    getRequestMap(request), getParameterMap(request),
                    getSessionMap(request), getApplicationMap(),
                    portletNamespace, EVENT_PHASE);
            LOG.debug("Leaving processAction");
        } finally {
            ActionContext.setContext(null);
        }
    }

    /**
     * Service an action from the <tt>render</tt> phase.
     *
     * @see javax.portlet.Portlet#render(javax.portlet.RenderRequest,
     *      javax.portlet.RenderResponse)
     */
    public void render(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {

        LOG.debug("Entering render");
        resetActionContext();
        response.setTitle(getTitle(request));
        try {
            // Check to see if an event set the render to be included directly
            serviceAction(request, response, getActionMapping(request),
                    getRequestMap(request), getParameterMap(request),
                    getSessionMap(request), getApplicationMap(),
                    portletNamespace, RENDER_PHASE);
            LOG.debug("Leaving render");
        } finally {
            resetActionContext();
        }
    }

    /**
     *  Reset the action context.
     */
    private void resetActionContext() {
        ActionContext.setContext(null);
    }

    /**
     * Merges all application and portlet attributes into a single
     * <tt>HashMap</tt> to represent the entire <tt>Action</tt> context.
     *
     * @param requestMap a Map of all request attributes.
     * @param parameterMap a Map of all request parameters.
     * @param sessionMap a Map of all session attributes.
     * @param applicationMap a Map of all servlet context attributes.
     * @param request the PortletRequest object.
     * @param response the PortletResponse object.
     * @param portletConfig the PortletConfig object.
     * @param phase The portlet phase (render or action, see
     *        {@link PortletActionConstants})
     * @return a HashMap representing the <tt>Action</tt> context.
     */
    public HashMap createContextMap(Map requestMap, Map parameterMap,
            Map sessionMap, Map applicationMap, PortletRequest request,
            PortletResponse response, PortletConfig portletConfig, Integer phase) throws IOException {

        // TODO Must put http request/response objects into map for use with
        // ServletActionContext
        HttpServletResponse dummyResponse = new PortletServletResponse(response);
        HttpServletRequest dummyRequest = new PortletServletRequest(request, getPortletContext());
        ServletContext dummyServletContext = new PortletServletContext(getPortletContext());
        if(EVENT_PHASE.equals(phase)) {
            dummyRequest = dispatcherUtils.wrapRequest(dummyRequest, dummyServletContext);
        }
        HashMap<String,Object> extraContext = new HashMap<String,Object>();
        // The dummy servlet objects. Eases reuse of existing interceptors that uses the servlet objects.
        extraContext.put(StrutsStatics.HTTP_REQUEST, dummyRequest);
        extraContext.put(StrutsStatics.HTTP_RESPONSE, dummyResponse);
        extraContext.put(StrutsStatics.SERVLET_CONTEXT, dummyServletContext);
        // End dummy servlet objects
        extraContext.put(ActionContext.PARAMETERS, parameterMap);
        extraContext.put(ActionContext.SESSION, sessionMap);
        extraContext.put(ActionContext.APPLICATION, applicationMap);

        String defaultLocale = dispatcherUtils.getContainer().getInstance(String.class, StrutsConstants.STRUTS_LOCALE);
        Locale locale = null;
        if (defaultLocale != null) {
            locale = LocalizedTextUtil.localeFromString(defaultLocale, request.getLocale());
        } else {
            locale = request.getLocale();
        }
        extraContext.put(ActionContext.LOCALE, locale);

        extraContext.put(StrutsStatics.STRUTS_PORTLET_CONTEXT, getPortletContext());
        extraContext.put(REQUEST, request);
        extraContext.put(RESPONSE, response);
        extraContext.put(PORTLET_CONFIG, portletConfig);
        extraContext.put(PORTLET_NAMESPACE, portletNamespace);
        extraContext.put(DEFAULT_ACTION_FOR_MODE, actionMap.get(request.getPortletMode()));
        // helpers to get access to request/session/application scope
        extraContext.put("request", requestMap);
        extraContext.put("session", sessionMap);
        extraContext.put("application", applicationMap);
        extraContext.put("parameters", parameterMap);
        extraContext.put(MODE_NAMESPACE_MAP, modeMap);

        extraContext.put(PHASE, phase);

        AttributeMap attrMap = new AttributeMap(extraContext);
        extraContext.put("attr", attrMap);

        return extraContext;
    }

    /**
     * Loads the action and executes it. This method first creates the action
     * context from the given parameters then loads an <tt>ActionProxy</tt>
     * from the given action name and namespace. After that, the action is
     * executed and output channels throught the response object.
     *
     * @param request the HttpServletRequest object.
     * @param response the HttpServletResponse object.
     * @param mapping the action mapping.
     * @param requestMap a Map of request attributes.
     * @param parameterMap a Map of request parameters.
     * @param sessionMap a Map of all session attributes.
     * @param applicationMap a Map of all application attributes.
     * @param portletNamespace the namespace or context of the action.
     * @param phase The portlet phase (render or action, see
     *        {@link PortletActionConstants})
     */
    public void serviceAction(PortletRequest request, PortletResponse response,
            ActionMapping mapping, Map requestMap, Map parameterMap,
            Map sessionMap, Map applicationMap, String portletNamespace,
            Integer phase) throws PortletException {
        LOG.debug("serviceAction");
        String actionName = mapping.getName();
        String namespace = mapping.getNamespace();
        Dispatcher.setInstance(dispatcherUtils);
        try {
            HashMap extraContext = createContextMap(requestMap, parameterMap,
                    sessionMap, applicationMap, request, response,
                    getPortletConfig(), phase);
            LOG.debug("Creating action proxy for name = " + actionName
                    + ", namespace = " + namespace);
            ActionProxy proxy = factory.createActionProxy(namespace,
                    actionName, extraContext);
            proxy.setMethod(mapping.getMethod());
            request.setAttribute("struts.valueStack", proxy.getInvocation()
                    .getStack());
            proxy.execute();
        } catch (ConfigurationException e) {
            LOG.error("Could not find action", e);
            throw new PortletException("Could not find action " + actionName, e);
        } catch (Exception e) {
            LOG.error("Could not execute action", e);
            throw new PortletException("Error executing action " + actionName,
                    e);
        } finally {
            Dispatcher.setInstance(null);
        }
    }

    /**
     * Returns a Map of all application attributes. Copies all attributes from
     * the {@link PortletActionContext}into an {@link ApplicationMap}.
     *
     * @return a Map of all application attributes.
     */
    protected Map getApplicationMap() {
        return new PortletApplicationMap(getPortletContext());
    }

    /**
     * Gets the namespace of the action from the request. The namespace is the
     * same as the portlet mode. E.g, view mode is mapped to namespace
     * <code>view</code>, and edit mode is mapped to the namespace
     * <code>edit</code>
     *
     * @param request the PortletRequest object.
     * @return the namespace of the action.
     */
    protected ActionMapping getActionMapping(final PortletRequest request) {
        ActionMapping mapping = null;
        String actionPath = null;
        if (resetAction(request)) {
            mapping = actionMap.get(request.getPortletMode());
        } else {
            actionPath = request.getParameter(ACTION_PARAM);
            if (!TextUtils.stringSet(actionPath)) {
                mapping = actionMap.get(request
                        .getPortletMode());
            } else {
                
                // Use the usual action mapper, but it is expecting an action extension
                // on the uri, so we add the default one, which should be ok as the
                // portlet is a portlet first, a servlet second
                PortletServletRequest httpRequest = new PortletServletRequest(request, getPortletContext());
                mapping = actionMapper.getMapping(httpRequest, dispatcherUtils.getConfigurationManager());
            }
        }
        
        if (mapping == null) {
            throw new StrutsException("Unable to locate action mapping for request, probably due to " +
                    "an invalid action path: "+actionPath);
        }
        return mapping;
    }

    /**
     * Get the namespace part of the action path.
     * @param actionPath Full path to action
     * @return The namespace part.
     */
    String getNamespace(String actionPath) {
        int idx = actionPath.lastIndexOf('/');
        String namespace = "";
        if (idx >= 0) {
            namespace = actionPath.substring(0, idx);
        }
        return namespace;
    }

    /**
     * Get the action name part of the action path.
     * @param actionPath Full path to action
     * @return The action name.
     */
    String getActionName(String actionPath) {
        int idx = actionPath.lastIndexOf('/');
        String action = actionPath;
        if (idx >= 0) {
            action = actionPath.substring(idx + 1);
        }
        return action;
    }

    /**
     * Returns a Map of all request parameters. This implementation just calls
     * {@link PortletRequest#getParameterMap()}.
     *
     * @param request the PortletRequest object.
     * @return a Map of all request parameters.
     * @throws IOException if an exception occurs while retrieving the parameter
     *         map.
     */
    protected Map getParameterMap(PortletRequest request) throws IOException {
        return new HashMap(request.getParameterMap());
    }

    /**
     * Returns a Map of all request attributes. The default implementation is to
     * wrap the request in a {@link RequestMap}. Override this method to
     * customize how request attributes are mapped.
     *
     * @param request the PortletRequest object.
     * @return a Map of all request attributes.
     */
    protected Map getRequestMap(PortletRequest request) {
        return new PortletRequestMap(request);
    }

    /**
     * Returns a Map of all session attributes. The default implementation is to
     * wrap the reqeust in a {@link SessionMap}. Override this method to
     * customize how session attributes are mapped.
     *
     * @param request the PortletRequest object.
     * @return a Map of all session attributes.
     */
    protected Map getSessionMap(PortletRequest request) {
        return new PortletSessionMap(request);
    }

    /**
     * Convenience method to ease testing.
     * @param factory
     */
    protected void setActionProxyFactory(ActionProxyFactory factory) {
        this.factory = factory;
    }

    /**
     * Check to see if the action parameter is valid for the current portlet mode. If the portlet
     * mode has been changed with the portal widgets, the action name is invalid, since the
     * action name belongs to the previous executing portlet mode. If this method evaluates to
     * <code>true</code> the <code>default&lt;Mode&gt;Action</code> is used instead.
     * @param request The portlet request.
     * @return <code>true</code> if the action should be reset.
     */
    private boolean resetAction(PortletRequest request) {
        boolean reset = false;
        Map paramMap = request.getParameterMap();
        String[] modeParam = (String[]) paramMap.get(MODE_PARAM);
        if (modeParam != null && modeParam.length == 1) {
            String originatingMode = modeParam[0];
            String currentMode = request.getPortletMode().toString();
            if (!currentMode.equals(originatingMode)) {
                reset = true;
            }
        }
        if(reset) {
        	request.setAttribute(ACTION_RESET, Boolean.TRUE);
        }
        else {
        	request.setAttribute(ACTION_RESET, Boolean.FALSE);
        }
        return reset;
    }

    public void destroy() {
        if (dispatcherUtils == null) {
            LOG.warn("something is seriously wrong, DispatcherUtil is not initialized (null) ");
        } else {
            dispatcherUtils.cleanup();
        }
    }

    /**
     * @param actionMapper the actionMapper to set
     */
    public void setActionMapper(ActionMapper actionMapper) {
        this.actionMapper = actionMapper;
    }
    
}