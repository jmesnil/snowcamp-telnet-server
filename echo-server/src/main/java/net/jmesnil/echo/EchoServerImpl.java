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
package net.jmesnil.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
class EchoServerImpl implements EchoServer {
    private final ServerSocket socket;
    private final ExecutorService executor;
    private boolean reverse;
    private boolean upperCase;

    private final ConcurrentMap<String, Socket> clientSockets = new ConcurrentHashMap<String, Socket>();

    EchoServerImpl(ServerSocket socket, ExecutorService executor, boolean reverse, boolean upperCase) {
        this.socket = socket;
        this.executor = executor;
        this.reverse = reverse;
        this.upperCase = upperCase;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public boolean isUpperCase() {
        return upperCase;
    }

    public void setUpperCase(boolean upperCase) {
        this.upperCase = upperCase;
    }

    public void start() {
        executor.execute(new Runnable() {
            public void run() {
                while (!socket.isClosed()) {
                    try {
                        final Socket s = socket.accept();
                        System.out.println("s = " + s.getRemoteSocketAddress().toString());
                        clientSockets.putIfAbsent(s.getRemoteSocketAddress().toString(), s);
                        executor.execute(new Runnable() {
                            public void run() {
                                try {
                                    final BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                    final PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    while (!s.isClosed()) {
                                        String line = reader.readLine();
                                        if ("bye".equalsIgnoreCase(line)) {
                                            out.println("Enjoy the \uD83C\uDFC2");
                                            clientSockets.remove(s.getRemoteSocketAddress().toString());
                                            s.close();
                                            return;
                                        }
                                        if (upperCase) {
                                            line = line.toUpperCase();
                                        }
                                        if (reverse) {
                                            line = new StringBuilder(line).reverse().toString();
                                        }
                                        out.println(line);
                                    }
                                } catch (IOException e) {
                                    if (!s.isClosed()) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (SocketException e) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void stop() {
        executor.shutdown();
        try {
            closeClients();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void closeClients() throws IOException {
        for (Socket socket : clientSockets.values()) {
            try (
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("Enjoy the \uD83C\uDFC2");
                socket.close();
            }
        }
        clientSockets.clear();
    }

    public Set<String> getClients() {
        return new HashSet<>(clientSockets.keySet());

    }
    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(9999);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        EchoServerImpl server = new EchoServerImpl(socket, executor, true, true);
        server.start();
    }
}
