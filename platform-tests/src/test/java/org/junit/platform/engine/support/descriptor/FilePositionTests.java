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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FilePositionTests {

	@Test
	void equalsAndHashCode() {
		FilePosition equal1 = new FilePosition(42, 23);
		FilePosition equal2 = new FilePosition(42, 23);
		FilePosition different = new FilePosition(23, 42);

		assertNotNull(equal1);
		assertNotNull(equal2);
		assertNotNull(different);

		assertNotSame(equal1, equal2);
		assertFalse(equal1.equals(null));
		assertFalse(equal1.equals(different));
		assertFalse(different.equals(equal1));
		assertNotEquals(equal1.hashCode(), different.hashCode());

		assertTrue(equal1.equals(equal1));
		assertTrue(equal1.equals(equal2));
		assertTrue(equal2.equals(equal1));
		assertEquals(equal1.hashCode(), equal2.hashCode());
	}
}
