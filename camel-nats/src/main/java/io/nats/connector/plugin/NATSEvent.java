/*******************************************************************************
 * Copyright (c) 2012, 2016 Apcera Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License (MIT)
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *******************************************************************************/

package io.nats.connector.plugin;

/**
 * Enumeration of NATSEvents passed to the plugin.
 */
public enum NATSEvent {

    /**
     * An asynchronous error has occurred.
     */
    ASYNC_ERROR,

    /***
     * The server has disconnected.
     */
    DISCONNECTED,

    /***
     * The connection to the NATS cluster has been closed.
     */
    CLOSED,

    /**
     * The server has reconnected.
     */
    RECONNECTED
}
