

package io.nats.connector;

import io.nats.client.*;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

public class NatsConnector implements MessageHandler, Runnable {

    private CamelNatsAdapter 	camelNatsAdapter = null;
    private Subscription     	subscription = null;
    private Properties       	properties = null;
    
    private Logger            	logger     = null;
    private AtomicBoolean     	isRunning   = new AtomicBoolean();
    private Object              runningLock = new Object();

    private ConnectionFactory 	connectionFactory = null;
    private Connection        	connection        = null;

    public NatsConnector(CamelNatsAdapter adapter, Properties props, Logger logger)
    {
        this.camelNatsAdapter = adapter;
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
                camelNatsAdapter.onReconnect(event);
            }
            catch (Exception e) {
                logger.error("Runtime exception in plugin method OnNATSEvent (RECONNECTED): ", e);
            }
        }

        @Override
        public void onClose(ConnectionEvent event)
        {
            try {
                camelNatsAdapter.onClose(event);
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

                camelNatsAdapter.onException(ex);
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

                camelNatsAdapter.onDisconnect(event);
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


        camelNatsAdapter.onStartup(logger);

        connection = connectionFactory.createConnection();
        logger.debug("Connected to NATS cluster.");
    }

    private void teardown()
    {
    	invokePluginShutdown();
        try
        {
            if (subscription != null)
            {
            	subscription.unsubscribe();
                subscription.close();
                subscription = null;
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

    public void onMessage(Message m)
    {
        logger.debug("Received Message:" + m.toString());
        
        try
        {
            camelNatsAdapter.onNATSMessage(m);
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (onMessage): ", e);
        }
    }


    private boolean invokeOnNatsInitialized()
    {
        logger.trace("OnNatsInitialized");
        try
        {
            return camelNatsAdapter.onNatsInitialized(this);
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
            camelNatsAdapter.onShutdown();
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (OnShutdown): ", e);
        }
    }
    @Override
    public void run()
    {
        logger.info("Setting up NATS Connector.");

        try {
            // connect to the NATS cluster
            setup();
        }
        catch (Exception e) {
            logger.error("Setup error: " + e.getMessage());
            logger.debug("Exception: ", e);
            teardown();
            return;
        }
        
        boolean run = true;

        if (!invokeOnNatsInitialized())
        {
            logger.error("Plugin failed to start.  Exiting.");
            teardown();
            return;
        }

        logger.info("The NATS Connector is running.");


        isRunning.set(true);

        while (run)
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

                run = isRunning.get();
            }
        }


        teardown();
    }

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
        if ( isRunning.get() == false )
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
    
    public void autoUnsubscribe(String subject, int max)
    {
        if (subject == null)
            return;

     
        logger.debug("Plugin unsubscribe after max num of messages from '{}'.", subject);

        if (subscription == null) {
            logger.debug("Subscription not found.");
            return;
        }
        else if(!subscription.getSubject().equalsIgnoreCase(subject)){
            logger.debug("Subscription not found.");
            return;
        }
        else{
	        try {
	        	subscription.autoUnsubscribe(max);
	        } catch (Exception e) {
	            logger.debug("Plugin unsubscribe failed.", e);
	            return;
	        }
        }   
    }
            

    public void subscribe(String subject, String queue, MessageHandler handler) throws Exception {

        if (subject == null)
            return;
        
        if (subscription != null && subscription.getSubject().equalsIgnoreCase(subject)) {
            logger.debug("Subscription already exists.");
            return;
        }

        AsyncSubscription s;

        if (queue == null)
            s = connection.subscribeAsync(subject, handler);
        else
            s = connection.subscribeAsync(subject, queue, handler);

        s.start();
         
    }

    public void subscribe(String subject, String queue) throws Exception {
        subscribe(subject, queue, this);
    }

    public void unsubscribe(String subject)
    {
        if (subject == null)
            return;

       
            logger.debug("Plugin unsubscribe from '{}'.", subject);

            if (subscription == null) {
                logger.debug("Subscription not found.");
                return;
            }

            try {
            	subscription.unsubscribe();
            } catch (Exception e) {
                logger.debug("Plugin unsubscribe failed.", e);
                return;
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
