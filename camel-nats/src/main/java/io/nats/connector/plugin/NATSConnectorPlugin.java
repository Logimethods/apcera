/*******************************************************************************
 * Copyright (c) 2012, 2016 Apcera Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.connector.plugin;

import io.nats.client.ConnectionFactory;
import org.slf4j.Logger;

/**
 * This interface that must be implemented for a NATS Connector plugin.
 * <p>
 * The order of calls are:
 * <p>
 * onStartup
 * <p>
 * onNatsIntialized
 * <p>
 * During operation:  onNatsMessage and onNATSEvent
 * <p>
 * onShutdown
 */
public interface NATSConnectorPlugin {

    /**
     * Invoked when the connector is started up, before a connection
     * to the NATS cluster is made.  The NATS connection factory is
     * valid at this time, providing an opportunity to alter
     * NATS connection parameters based on other plugin variables.
     *
     * @param logger - logger for the NATS connector process.
     * @param factory - the NATS connection factory.
     * @return - true if the connector should continue, false otherwise.
     */
    public boolean onStartup(Logger logger, ConnectionFactory factory);

    /**
     * Invoked after startup, when the NATS plug-in has connectivity to the
     * NATS cluster, and is ready to start sending and
     * and receiving messages.  This is the place to create NATS subscriptions.
     *
     * @param connector interface to the NATS connector
     *
     * @return true if the plugin can continue.
     */
    public boolean onNatsInitialized(NATSConnector connector);

    /**
     * Invoked anytime a NATS message is received to be processed.
     * @param msg - NATS message received.
     */
    public void onNATSMessage(io.nats.client.Message msg);

    /**
     * Invoked anytime a NATS event occurs around a connection
     * or error, alerting the plugin to take appropriate action.
     *
     * For example, when NATS is reconnecting, buffer or pause incoming
     * data to be sent to NATS.
     *
     * @param event the type of event
     * @param message a string describing the event
     */
    public void onNATSEvent(NATSEvent event, String message);


    /**
     * Invoked when the Plugin is shutting down.  This is typically where
     * plugin resources are cleaned up.
     */
    public void onShutdown();

}
