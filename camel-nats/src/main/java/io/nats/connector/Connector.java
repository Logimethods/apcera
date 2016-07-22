/*******************************************************************************
 * Copyright (c) 2012, 2016 Apcera Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.connector;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nats.connector.plugin.NATSConnectorPlugin;


/**
 * This is the main connector class for the NATS Connector.
 * <p>
 * It operates as follows:
 *<p>
 * The connector is entirely java properties driven - these can be set as parameters
 * to the JVM or passed in as a file.
 * <p>
 * The Connector starts a thread which drives a DataFlowHandler.
 * The handler connects to NATS and invokes various interfaces on
 * a supplied plugin.
 * <p>
 * The plugin has a few APIs that allow it to publish messages,
 * flush the NATS connection, and subscribe to various subjects.
 * <p>
 * A plugin can both subscribe to receive data and export it to another
 * system, feed data into NATS, or both.
 * <p>
 * The plugin's responsibilities include: a) Ensuring performance out of nats.
 * This may include some buffering if the destination of the data consumer
 * slower than NATS produces it. b) Translation of external origin/destination
 * and the subject namespace.
 */
public class Connector implements Runnable
{
   
    static final Logger logger = LoggerFactory.getLogger(Connector.class);


	public void setPlugin(NATSConnectorPlugin plugin) {
		this.plugin = plugin;
	}

	NATSConnectorPlugin plugin = null;
    Properties          gwProps = null;
    String              configFile = null;
    DataFlowHandler     flowHandler = null;


   
    @Override
    public void run()
    {
        try
        {
            logger.info("NATS Connector starting up.");

            flowHandler = new DataFlowHandler(plugin, gwProps, logger);
          
            Runtime.getRuntime().addShutdownHook(
                    new Thread()
                    {
                        public void run()
                        {
                            logger.debug("Cleaning up from shutdown hook.");
                            flowHandler.cleanup();
                        }
                    });

            flowHandler.process();
          

            logger.info("NATS Connector has shut down.");
        }
        catch (Exception e)
        {
            logger.error("Exception:  ", e.getMessage());
            throw e;
        }
    }

    /**
     * Shuts down a running Connector.
     */
    public void shutdown()
    {
        if (flowHandler == null)
            return;

        flowHandler.shutdown();
    }

	public Connector(Properties properties) throws Exception {
        this.gwProps = properties;
		
    }

}
