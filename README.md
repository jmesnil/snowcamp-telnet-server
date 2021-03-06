# snowcamp-telnet-server

Demo for WildFly Extension workshop.

This project provides a dummy echo server that requires a `ServerSocket` and an `ExecutorService` to handle client connections.
It can be configured to reverse client requests and/or change it to upper case.

The main goal of this demo is to show how WildFly Core can be sued to integrate such a server in WildFly and leverage it to provide
resources (such as socket bindings and thread pools) and manage it using WildFly command-line interface or XML configuration.

# Build Instructions

* run `mvn clean install` from the root directory

# Integration with WildFly Core

* copy `extension/src/main/resources/modules` directory to `$WFLYCORE_HOME/modules` directory.
* start WildFly core: `./bin/standalone.sh`
* add the extension using WildFly CLI : `/extension=net.jmesnil.echo.extension:add(module=net.jmesnil.echo.extension)`
* add the subsystem: `/subsystem=echo:add()`
* add a socket-binding: `/socket-binding-group=standard-sockets/socket-binding=echo1:add(port=9999)`
* add a echo server: `/subsystem=echo/server=server1:add(reverse=true, socket-binding=telnet1)`
