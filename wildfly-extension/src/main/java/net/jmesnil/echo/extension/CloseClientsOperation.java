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

import net.jmesnil.echo.EchoServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class CloseClientsOperation implements OperationStepHandler {

    static final CloseClientsOperation INSTANCE = new CloseClientsOperation();

    public void execute(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {
        String name = operationContext.getCurrentAddressValue();
        ServiceController<EchoServer> service = (ServiceController<EchoServer>) operationContext.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
        EchoServer echoServer = service.getValue();
        try {
            echoServer.closeClients();
        } catch (IOException e) {
            throw new OperationFailedException(e);
        }
    }

    public static void register(ManagementResourceRegistration resourceRegistration) {
        SimpleOperationDefinition definition = new SimpleOperationDefinitionBuilder("close-clients", EchoExtension.getResolver())
                .setRuntimeOnly()
                .build();
        resourceRegistration.registerOperationHandler(definition, INSTANCE);
    }
}
