/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine.support.descriptor;

import java.io.Serializable;
import java.util.Optional;

import org.junit.platform.engine.LegacyReportingInfo;

public class DefaultLegacyReportingInfo implements LegacyReportingInfo, Serializable {
	private static final long serialVersionUID = 2896941058543948169L;
	private final String methodName;
	private final String className;

	public static DefaultLegacyReportingInfo from(LegacyReportingInfo info) {
		String methodName = info.getName().orElse(null);
		String className = info.getClassName().orElse(null);
		return new DefaultLegacyReportingInfo(methodName, className);
	}

	public DefaultLegacyReportingInfo(String methodName, String className) {
		this.methodName = methodName;
		this.className = className;
	}

	@Override
	public Optional<String> getName() {
		return Optional.ofNullable(methodName);
	}

	@Override
	public Optional<String> getClassName() {
		return Optional.ofNullable(className);
	}
}
