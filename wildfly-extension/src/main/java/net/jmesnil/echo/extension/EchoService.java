/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package net.jmesnil.echo.extension;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.jmesnil.echo.EchoServer;
import net.jmesnil.echo.EchoServerFactory;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
class EchoService implements Service<EchoServer> {

    static final ServiceName SERVER = ServiceName.of("net.jmesnil.echo");

    protected final InjectedValue<SocketBinding> binding = new InjectedValue<SocketBinding>();
    private final boolean upperCase;
    private final boolean reverse;

    public InjectedValue<SocketBinding> getBinding() {
        return binding;
    }

    private ServerSocket serverSocket =  null;
    private EchoServer server = null;

    EchoService(boolean upperCase, boolean reverse) {
        this.upperCase = upperCase;
        this.reverse = reverse;
    }

    public void start(StartContext context) throws StartException {
        try {
            serverSocket = binding.getValue().createServerSocket();
            ExecutorService executor = Executors.newFixedThreadPool(5);
            server = EchoServerFactory.newServer(serverSocket, executor, reverse, upperCase);
            server.start();
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    public void stop(StopContext context) {
        server.stop();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public EchoServer getValue() throws IllegalStateException, IllegalArgumentException {
        return server;
    }
}
