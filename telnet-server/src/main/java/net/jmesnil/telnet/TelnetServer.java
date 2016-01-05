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
package net.jmesnil.telnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class TelnetServer {
    private final ServerSocket socket;
    private final ExecutorService executor;
    private boolean reverse;
    private boolean upperCase;

    public TelnetServer(ServerSocket socket, ExecutorService executor, boolean reverse, boolean upperCase) {
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
                        executor.execute(new Runnable() {
                            public void run() {
                                try {
                                    final BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                    final PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                                    while (!s.isClosed()) {
                                        String line = reader.readLine();
                                        if ("bye".equalsIgnoreCase(line)) {
                                            out.println("Enjoy the \uD83C\uDFC2");
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
                                    e.printStackTrace();
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
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(9999);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        TelnetServer server = new TelnetServer(socket, executor, true, true);
        server.start();


    }
}
