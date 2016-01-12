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

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
public class EchoExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "echo";

    static final PathElement SUBSYSTEM_PATH = pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement SERVER_PATH = pathElement("server");

    private static final String RESOURCE_NAME = EchoExtension.class.getPackage().getName() + ".LocalDescriptions";
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, EchoExtension.class.getClassLoader(), true, false);

    }

    public void initialize(ExtensionContext extensionContext) {
        final SubsystemRegistration subsystemRegistration = extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystemRegistration.registerXMLElementWriter(EchoSubsystemParser_1_0.INSTANCE);

        final ManagementResourceRegistration subsystem = subsystemRegistration.registerSubsystemModel(EchoSubsystemDefinition.INSTANCE);
        subsystem.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

    }

    public void initializeParsers(ExtensionParsingContext extensionParsingContext) {
        extensionParsingContext.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TELNET_1_0.getName(), EchoSubsystemParser_1_0.INSTANCE);
    }
}
