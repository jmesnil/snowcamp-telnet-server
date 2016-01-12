package net.jmesnil.echo;

import java.io.IOException;
import java.util.Set;

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

    Set<String> getClients();

    void closeClients() throws IOException;
}
