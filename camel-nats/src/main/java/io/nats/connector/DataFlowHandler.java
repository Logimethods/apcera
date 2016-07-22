/*******************************************************************************
 * Copyright (c) 2012, 2016 Apcera Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.connector;

import io.nats.client.*;
import io.nats.connector.plugin.NATSConnector;
import io.nats.connector.plugin.NATSConnectorPlugin;
import io.nats.connector.plugin.NATSEvent;

import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataFlowHandler implements MessageHandler, NATSConnector {

    private NATSConnectorPlugin plugin     = null;

    // Rely heavily on NATS locking, but protect data structures here
    // with the plugin lock.
    private Object              pluginLock   = new Object();
    private HashMap             subscriptions = new HashMap<String, Subscription>();

    private Properties          properties = null;
    private Logger              logger     = null;

    private Object              runningLock = new Object();

    private Object cleanupLock = new Object();
    private boolean hasCleanedUp = false;

    // TODO eval - this for performance.  Is it necessary?
    private AtomicBoolean     isRunning   = new AtomicBoolean();

    private ConnectionFactory connectionFactory = null;
    private Connection        connection        = null;

    public DataFlowHandler(NATSConnectorPlugin plugin, Properties props, Logger logger)
    {
        this.plugin = plugin;
        this.properties = props;
        this.logger = logger;
    }

    class EventHandlers implements ClosedCallback, DisconnectedCallback,
            ExceptionHandler, ReconnectedCallback
    {
        @Override
        public void onReconnect(ConnectionEvent event)
        {
            try {
                String desc = "NATS Connection reconnected.";
                logger.info(desc);
                plugin.onNATSEvent(NATSEvent.RECONNECTED, desc);
            }
            catch (Exception e) {
                logger.error("Runtime exception in plugin method OnNATSEvent (RECONNECTED): ", e);
            }
        }

        @Override
        public void onClose(ConnectionEvent event)
        {
            try {
                plugin.onNATSEvent(NATSEvent.CLOSED, "NATS Connection closed.");
            }
            catch (Exception e) {
                logger.error("Runtime exception in plugin method OnNATSEvent (CLOSED): ", e);
            }
        }

        public void onException(NATSException ex)
        {
            try {
                logger.error("Asynchronous error: exception: {}",
                        ex.getMessage());

                plugin.onNATSEvent(NATSEvent.ASYNC_ERROR, ex.getMessage());
            }
            catch (Exception e) {
                logger.error("Runtime exception in plugin method OnNATSEvent (EXCEPTION): ", e);
            }
        }

        @Override
        public void onDisconnect(ConnectionEvent event) {
            try {

                String desc = "NATS Connection disconnected.";
                logger.debug(desc);

                plugin.onNATSEvent(NATSEvent.DISCONNECTED, desc);
            }
            catch (Exception e) {
                logger.error("Runtime exception in plugin method OnNATSEvent (DISCONNECTED): ", e);
            }
        }
    }

    private void setup() throws Exception
    {
        connectionFactory = new ConnectionFactory(properties);
        EventHandlers eh = new EventHandlers();

        connectionFactory.setClosedCallback(eh);
        connectionFactory.setDisconnectedCallback(eh);
        connectionFactory.setExceptionHandler(eh);
        connectionFactory.setReconnectedCallback(eh);

        // invoke on startup here, so the user can override or set their
        // own callbacks in the plugin if need be.
        if (invokeOnStartup(connectionFactory) == false) {
            shutdown();
            throw new Exception("Startup failure initiated From plug-in");
        }

        connection = connectionFactory.createConnection();
        logger.debug("Connected to NATS cluster.");
    }

    private void teardown()
    {
        try
        {
            if (subscriptions != null)
            {
                for (Object s : subscriptions.values())
                    ((Subscription) s).unsubscribe();

                subscriptions.clear();
                subscriptions = null;
            }
        }
        catch (Exception e) {}

        try
        {
            if (connection != null)
                 connection.close();
        }
        catch (Exception e) {}

        logger.debug("Closed connection to NATS cluster.");
    }

    /*
     *   TODO:  How well can request reply work?  Need additional support?
     */
    public void onMessage(Message m)
    {
        try
        {
            plugin.onNATSMessage(m);
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (onMessage): ", e);
        }
    }

    private boolean invokeOnStartup(ConnectionFactory factory)
    {

        logger.debug("OnStartup");
        try
        {
            return plugin.onStartup(LoggerFactory.getLogger(plugin.getClass().getName()), factory);
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (OnStartup): ", e);
            return false;
        }
    }

    private boolean invokeOnNatsInitialized()
    {
        logger.trace("OnNatsInitialized");
        try
        {
            return plugin.onNatsInitialized(this);
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (OnNatsInitialized): ", e);
            return false;
        }
    }

    private void invokePluginShutdown()
    {
        logger.trace("OnShutdown");
        try
        {
            plugin.onShutdown();
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (OnShutdown): ", e);
        }
    }

    public void process()
    {
        logger.debug("Setting up NATS Connector.");

        boolean running = true;

        try {
            // connect to the NATS cluster
            setup();
        }
        catch (Exception e) {
            logger.error("Setup error: " + e.getMessage());
            logger.debug("Exception: ", e);
            cleanup();
            return;
        }

        if (!invokeOnNatsInitialized())
        {
            logger.error("Plugin failed to start.  Exiting.");
            cleanup();
            return;
        }

        logger.info("The NATS Connector is running.");

        isRunning.set(true);

        while (running)
        {
            synchronized(runningLock)
            {
                try {
                    runningLock.wait();
                }
                catch (InterruptedException e) {
                    // As of java 1.6, Object.wait can be woken up spuriously,
                    // so we need to check if we are still running.
                }

                running = isRunning.get();
            }
        }

        cleanup();
    }

    public void cleanup()
    {
        synchronized (cleanupLock)
        {
            if (hasCleanedUp)
                return;


            logger.debug("Cleaning up.");

            // we are shutting down.
            invokePluginShutdown();
            teardown();

            hasCleanedUp = true;
        }

        logger.debug("Cleaned up NATS Connector.");
    }

    /*
     * NATSConnector
     */
    public void publish(Message msg)
    {
        if (isRunning.get() == false)
            return;

        try {
            connection.publish(msg);
        }
        catch (Exception ex) {
            logger.error("Exception publishing: " + ex.getMessage());
            logger.debug("Exception: " + ex);
        }

    }

    public void flush() throws Exception
    {
        // if the connector is shutting down, then we silently fail.
        if (isRunning.get() == false)
            return;

        if (connection == null)
            throw new Exception("Invalid state.  Connection is null.");

        try {
            connection.flush();
        }
        catch (Exception ex)
        {
            throw new Exception("Unable to flush NATS connection.", ex);
        }
    }

    public void shutdown()
    {
        if (isRunning.get() == false)
            return;

        logger.debug("NATS connector is shutting down.");

        isRunning.set(false);

        synchronized (runningLock)
        {
            runningLock.notify();
        }
    }

    public void subscribe(String subject, MessageHandler handler) throws Exception
    {
        subscribe(subject, null, handler);
    }

    public void subscribe(String subject) throws Exception
    {
        subscribe(subject, null, this);
    }

    public void subscribe(String subject, String queue, MessageHandler handler) throws Exception {

        if (subject == null)
            return;

        synchronized (pluginLock)
        {
                logger.debug("Plugin subscribe to '{}', queue '{}'.", subject,
                    (queue == null ? "(none)" : queue));

            // do not subscribe twice.
            if (subscriptions.containsKey(subject)) {
                logger.debug("Subscription already exists.");
                return;
            }

            AsyncSubscription s;

            if (queue == null)
                s = connection.subscribeAsync(subject, handler);
            else
                s = connection.subscribeAsync(subject, queue, handler);

            s.start();

            subscriptions.put(subject, s);
        }
    }

    public void subscribe(String subject, String queue) throws Exception {
        subscribe(subject, queue, this);
    }

    public void unsubscribe(String subject)
    {
        if (subject == null)
            return;

        synchronized (pluginLock)
        {
            logger.debug("Plugin unsubscribe from '{}'.", subject);

            Subscription s = (Subscription) subscriptions.get(subject);
            if (s == null) {
                logger.debug("Subscription not found.");
                return;
            }

            try {
                s.unsubscribe();
            } catch (Exception e) {
                logger.debug("Plugin unsubscribe failed.", e);
                return;
            }
        }
    }
    
    public void autoUnsubscribe(String subject, int max)
    {
        if (subject == null)
            return;

        synchronized (pluginLock)
        {
            logger.debug("Plugin unsubscribe after max num of messages from '{}'.", subject);

            Subscription s = (Subscription) subscriptions.get(subject);
            if (s == null) {
                logger.debug("Subscription not found.");
                return;
            }

            try {
                s.autoUnsubscribe(max);
            } catch (Exception e) {
                logger.debug("Plugin unsubscribe failed.", e);
                return;
            }
        }
    }

    public Connection getConnection()
    {
        return connection;
    }

    public ConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

}
