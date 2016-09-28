/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.construct.Flow;
import org.mule.raml.interfaces.model.IResource;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Router extends AbstractRouter
{

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private ConsoleHandler consoleHandler;

    public Configuration getConfig()
    {
        return (Configuration) config;
    }

    public void setConfig(Configuration config)
    {
        this.config = config;
    }

    @Override
    protected void startConfiguration() throws StartException
    {
        if (config == null)
        {
            try
            {
                config = muleContext.getRegistry().lookupObject(Configuration.class);
            }
            catch (RegistrationException e)
            {
                throw new StartException(I18nMessageFactory.createStaticMessage("APIKit configuration not Found"), this);
            }
        }
        config.loadApiDefinition(flowConstruct);
        if (getConfig().isConsoleEnabled())
        {
            consoleHandler = new ConsoleHandler(getConfig().getEndpointAddress(flowConstruct), getConfig().getConsolePath(), config);
            consoleHandler.updateRamlUri();
            getConfig().addConsoleUrl(consoleHandler.getConsoleUrl());
        }
    }

    @Override
    protected Event handleEvent(Event event, String path) throws MuleException
    {
        //check for console request
        if (getConfig().isConsoleEnabled() && path.startsWith("/" + getConfig().getConsolePath()))
        {
            return consoleHandler.process(event);
        }
        return null;
    }

    /**
     * Returns the flow that handles the request or null if there is none.
     * First tries to match a flow by method, resource and content type,
     * if there is no match it retries using method and resource only.
     */
    @Override
    protected Flow getFlow(IResource resource, HttpRestRequest request)
    {
        String baseKey = request.getMethod() + ":" + resource.getUri();
        String contentType = request.getContentType();
        Map<String, Flow> rawRestFlowMap = ((Configuration) config).getRawRestFlowMap();
        Flow flow = rawRestFlowMap.get(baseKey + ":" + contentType);
        if (flow == null)
        {
            flow = rawRestFlowMap.get(baseKey);
        }
        return flow;
    }

    //TODO FIX METHOD OUTBOUND PROPERTIES
    //@Override
    protected Event doProcessRouterResponse(Event event, Integer successStatus)
    {
        //if (event.getMessage().getOutboundProperty("http.status") == null)
        //{
        //    event.getMessage().setOutboundProperty("http.status", successStatus);
        //}
        return event;
    }

}
