/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;


public class DefaultFrameworkConfiguration extends FrameworkConfiguration {

	@Override
	public void activateOsgiBuiltInConsole(String port) {
		addFrameworkProperty("osgi.console", "" + port);
	}

}
