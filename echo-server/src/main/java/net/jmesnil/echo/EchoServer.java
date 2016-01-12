package net.jmesnil.echo;

import java.io.IOException;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public interface EchoServer {

    void start();
    void stop();

    boolean isReverse();
    void setReverse(boolean reverse);

    boolean isUpperCase();
    void setUpperCase(boolean upperCase);

    void closeClients() throws IOException;
}
