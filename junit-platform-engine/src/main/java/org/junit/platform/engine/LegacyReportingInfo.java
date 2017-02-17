/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine;

import java.util.Optional;

import org.junit.platform.commons.meta.API;

/**
 * Represents a logical level display name resolution of a TestDescriptor for legacy reporting formats, instead of a source level name resolution.
 */
@API(API.Usage.Experimental)
public interface LegacyReportingInfo {
	/**
	 * @return The name of the descriptor if any, empty otherwise.
	 */
	Optional<String> getName();

	/**
	 * @return The name of the enclosing class of the descriptor if any, empty otherwise.
	 */
	Optional<String> getClassName();
}
