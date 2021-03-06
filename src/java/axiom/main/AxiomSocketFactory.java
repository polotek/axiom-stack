/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile: HelmaSocketFactory.java,v $
 * $Author: hannes $
 * $Revision: 1.5 $
 * $Date: 2003/12/15 17:07:06 $
 */

package axiom.main;

import java.io.IOException;
import java.net.*;
import java.rmi.server.*;

import axiom.util.*;

/**
 * An RMI socket factory that has a "paranoid" option to filter clients.
 * We only do direct connections, no HTTP proxy stuff, since this is
 * server-to-server.
 */
public class AxiomSocketFactory extends RMISocketFactory {
    private InetAddressFilter filter;

    /**
     * Creates a new AxiomSocketFactory object.
     */
    public AxiomSocketFactory() {
        filter = new InetAddressFilter();
    }

    /**
     *
     *
     * @param address ...
     */
    public void addAddress(String address) {
        try {
            filter.addAddress(address);
        } catch (IOException x) {
            Server.getServer().getLogger().error("Could not add " + address +
                                   " to Socket Filter: invalid address.");
        }
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    /**
     *
     *
     * @param port ...
     *
     * @return ...
     *
     * @throws IOException ...
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ParanoidServerSocket(port, filter);
    }
}
