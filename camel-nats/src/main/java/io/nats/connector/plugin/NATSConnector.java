/*******************************************************************************
 * Copyright (c) 2012, 2016 Apcera Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.connector.plugin;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import io.nats.client.MessageHandler;

/**
 * Interface for the utility class passed to the plug-in, allowing additional
 * NATS functionality such as publishing messsages and subscribing to
 * subjects.
 */
public interface NATSConnector {

    /**
     * In case of a critical failure or security issue, this allows the plugin
     * to request a shutdown of the connector.
     */
    public void shutdown();

    /***
     * Publishes a message into the NATS cluster.
     *
     * @param message - the message to publish.
     */
    public void publish(io.nats.client.Message message);

    /***
     * Flushes any pending NATS data.
     *
     * @throws  Exception - an error occurred in the flush.
     */
    public void flush() throws Exception;

    /***
     * Adds interest in a NATS subject.
     * @param subject - subject of interest.
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject) throws Exception;

    /***
     * Adds interest in a NATS subject, with a custom handle.
     * @param subject - subject of interest.
     * @param handler - message handler
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, MessageHandler handler) throws Exception;

    /***
     * Adds interest in a NATS subject with a queue group.
     * @param subject - subject of interest.
     * @param queue - work queue
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, String queue) throws Exception;

    /***
     * Adds interest in a NATS subject with a queue group, with a custom handler.
     * @param subject - subject of interest.
     * @param queue - work queue
     * @param handler - message handler
     * @throws Exception - an error occurred in the subsciption process.
     */
    public void subscribe(String subject, String queue, MessageHandler handler) throws Exception;

    /***
     * Removes interest in a NATS subject
     * @param subject - subject of interest.
     */
    public void unsubscribe(String subject);

    /***
     * Removes interest in a NATS subject after max number of messages
     * @param subject - subject of interest.
     */
    public void autoUnsubscribe(String subject, int max);
    
    /***
     * Advanced API to get the NATS connection.  This allows for NATS functionality beyond
     * the interface here.
     *
     * @return The connection to the NATS cluster.
     */
    public Connection getConnection();

    /***
     * Advanced API to get the Connection Factory, This allows for NATS functionality beyond
     * the interface here.
     *
     * @return The NATS connector ConnectionFactory
     */
    public ConnectionFactory getConnectionFactory();


}
