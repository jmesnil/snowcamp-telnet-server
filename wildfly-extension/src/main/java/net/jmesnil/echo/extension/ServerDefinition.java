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

import static org.jboss.dmr.ModelType.BOOLEAN;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import net.jmesnil.echo.EchoServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class ServerDefinition extends PersistentResourceDefinition {

    static final String SOCKET_CAPABILITY = "org.wildfly.network.socket-binding";

    private static final String LISTENER_CAPABILITY_NAME = "net.jmesnil.echo.server";
    static final RuntimeCapability<Void> LISTENER_CAPABILITY = RuntimeCapability.Builder.of(LISTENER_CAPABILITY_NAME, true, Service.class)
            .build();

    protected static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SOCKET_BINDING, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .setCapabilityReference(SOCKET_CAPABILITY, LISTENER_CAPABILITY)
            .build();
    static AttributeDefinition REVERSE = SimpleAttributeDefinitionBuilder.create("reverse", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();
    static AttributeDefinition UPPER_CASE = SimpleAttributeDefinitionBuilder.create("upper-case", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();
    static AttributeDefinition CLIENTS = new StringListAttributeDefinition.Builder("clients")
            .setStorageRuntime()
            .build();

    private ServerDefinition() {
        super(EchoExtension.SERVER_PATH,
                EchoExtension.getResolver("server"),
                new ServerAdd(),
                new ServerRemove());
    }

    private static final AttributeDefinition[] ATTRIBUTES = { SOCKET_BINDING, REVERSE, UPPER_CASE };

    static final ServerDefinition INSTANCE = new ServerDefinition();

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler defaultHandler = new ReloadRequiredWriteAttributeHandler(getAttributes());

        resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING, null, defaultHandler);
        resourceRegistration.registerReadWriteAttribute(UPPER_CASE, null, UpperCaseWriteHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(REVERSE, null, ReverseWriteHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(CLIENTS, ClientsAttributeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        CloseClientsOperation.register(resourceRegistration);
    }

    private static class ServerAdd extends AbstractAddStepHandler {
        ServerAdd() {
            super(LISTENER_CAPABILITY, ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            String bindingRef = SOCKET_BINDING.resolveModelAttribute(context, operation).asString();
            boolean upperCase = UPPER_CASE.resolveModelAttribute(context, operation).asBoolean();
            boolean reverse = REVERSE.resolveModelAttribute(context, operation).asBoolean();

            final ServiceName socketBindingServiceName = context.getCapabilityServiceName(ServerDefinition.SOCKET_CAPABILITY, bindingRef, SocketBinding.class);

            EchoService service = new EchoService(upperCase, reverse);
            context.getServiceTarget().addService(EchoService.SERVER.append(name), service)
                    .addDependency(socketBindingServiceName, SocketBinding.class, service.getBinding())
                    .install();
        }
    }

    private static class ServerRemove extends AbstractRemoveStepHandler {

    }

    private static class ClientsAttributeHandler extends AbstractRuntimeOnlyHandler {

        static final ClientsAttributeHandler INSTANCE = new ClientsAttributeHandler();

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            ServiceController<EchoServer> service = (ServiceController<EchoServer>) context.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
            EchoServer echoServer = service.getValue();
            Set<String> clients = echoServer.getClients();
            ModelNode result = new ModelNode();
            result.setEmptyList();
            for (String client : clients) {
                result.add(client);
            }
            context.getResult().set(result);

        }
    }

    private static class ReverseWriteHandler extends AbstractWriteAttributeHandler {
        static final ReverseWriteHandler INSTANCE = new ReverseWriteHandler();

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, AbstractWriteAttributeHandler.HandbackHolder handbackHolder) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            boolean reverse = resolvedValue.asBoolean();

            ServiceController<EchoServer> service = (ServiceController<EchoServer>) context.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
            EchoServer echoServer = service.getValue();
            echoServer.setReverse(reverse);

            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            boolean reverse = valueToRestore.asBoolean();

            ServiceController<EchoServer> service = (ServiceController<EchoServer>) context.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
            EchoServer echoServer = service.getValue();
            echoServer.setReverse(reverse);
        }
    }


    private static class UpperCaseWriteHandler extends AbstractWriteAttributeHandler {

        static final UpperCaseWriteHandler INSTANCE = new UpperCaseWriteHandler();

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            boolean upperCase = resolvedValue.asBoolean();

            ServiceController<EchoServer> service = (ServiceController<EchoServer>) context.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
            EchoServer echoServer = service.getValue();
            echoServer.setUpperCase(upperCase);

            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            boolean upperCase = valueToRestore.asBoolean();

            ServiceController<EchoServer> service = (ServiceController<EchoServer>) context.getServiceRegistry(false).getService(EchoService.SERVER.append(name));
            EchoServer echoServer = service.getValue();
            echoServer.setUpperCase(upperCase);
        }
    }
}
