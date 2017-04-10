/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.jupiter.migrationsupport.parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

class ParameterizedExtensionTests {

	@Test
	void parametrizedWithParameterFieldInjection() {
		ExecutionEventRecorder executionEventRecorder = executeTestsForClass(FibonacciTest.class);
		assertThat(executionEventRecorder.getTestSuccessfulCount()).isEqualTo(7);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class FibonacciTest {
		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(
				new Object[][] { { 0, 0 }, { 1, 1 }, { 2, 1 }, { 3, 2 }, { 4, 3 }, { 5, 5 }, { 6, 8 } });
		}

		@Parameterized.Parameter
		public int fInput;

		@Parameterized.Parameter(1)
		public int fExpected;

		@TestTemplate
		void test() {
			assertEquals(fExpected, compute(fInput));
		}

		private static int compute(int n) {
			int result = 0;

			if (n <= 1) {
				result = n;
			}
			else {
				result = compute(n - 1) + compute(n - 2);
			}

			return result;
		}
	}

	@Test
	void paremeterizedWithConstructorInjection() {
		ExecutionEventRecorder executionEventRecorder = executeTestsForClass(ParameterizedTestWithConstructor.class);
		assertThat(executionEventRecorder.getTestSuccessfulCount()).isEqualTo(7);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class ParameterizedTestWithConstructor {
		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(
				new Object[][] { { 0, 2 }, { 1, 3 }, { 2, 1 }, { 3, 2 }, { 4, 3 }, { 5, 8 }, { 6, 8 } });
		}

		private int a;
		private int b;

		public ParameterizedTestWithConstructor(int a, int b) {
			this.a = a;
			this.b = b;
		}

		@TestTemplate
		void test() {
			assertNotEquals(a, b);
		}
	}

	@Test
	void unMatchedConstructorArgumentCount() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(UnMatchedConstructor.class);
		assertThat(eventRecorder.getTestSuccessfulCount()).isEqualTo(0);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class UnMatchedConstructor {
		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 0, 2 } });
		}

		public UnMatchedConstructor(int a) {

		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void unMatchedParameterFieldsCount() {
		ExecutionEventRecorder executionEventRecorder = executeTestsForClass(WrongParameters.class);
		assertThat(exceptionsThrown(executionEventRecorder)).allSatisfy(
			e -> assertThat(e).isInstanceOf(ParameterResolutionException.class));
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class WrongParameters {
		@Parameterized.Parameter
		public int a;
		@Parameterized.Parameter(1)
		public int b;

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 0 }, { 1 }, { 1 }, { 3 }, { 4 }, { 5 }, { 6 } });
		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void noInjectionMix() {
		ParameterizedExtension extension = new ParameterizedExtension();

		ContainerExtensionContext containerContext = mock(ContainerExtensionContext.class);

		when(containerContext.getTestClass()).thenReturn(Optional.of(DoubleInjection.class));
		assertFalse(extension.supports(containerContext));
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class DoubleInjection {
		@Parameterized.Parameter
		public int a;

		public DoubleInjection(int a) {

		}

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { { 0 } });
		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void wrongReturnTypeFromParameters() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(BadParameterReturnType.class);
		assertThat(exceptionsThrown(eventRecorder)).allSatisfy(e -> {
			assertThat(e).isInstanceOf(ParameterResolutionException.class);
			assertThat(e).hasMessage("The @Parameters returns the wrong type");
		});
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class BadParameterReturnType {
		public BadParameterReturnType(int a) {

		}

		@Parameters
		public static int params() {
			return 0;
		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void emptyParametersList() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(EmptyParameters.class);
		assertThat(eventRecorder.getTestSuccessfulCount()).isEqualTo(2);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class EmptyParameters {

		public EmptyParameters() {
		}

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { {}, {} });
		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void duplicatedParameterFieldIndex() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(DuplicatedIndex.class);
		assertThat(eventRecorder.getTestSuccessfulCount()).isEqualTo(0);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class DuplicatedIndex {
		@Parameterized.Parameter
		public int a;

		@Parameterized.Parameter
		public int b;

		@Parameters
		public static Collection<Object[]> data() {
			return Arrays.asList(new Object[][] { {}, {} });
		}

		@TestTemplate
		void dummy() {

		}
	}

	@Test
	void parametersAreOnlyCalledOnce() {
		ExecutionEventRecorder executionEventRecorder = executeTestsForClass(ParametersCalledOnce.class);
		assertThat(executionEventRecorder.getTestSuccessfulCount()).isEqualTo(2);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class ParametersCalledOnce {
		private static int invocationCount = 0;

		public ParametersCalledOnce(int a) {

		}

		@Parameters
		public static Collection<Object[]> data() {
			invocationCount++;
			return Arrays.asList(new Object[][] { { 3 }, { 4 } });
		}

		@TestTemplate
		void dummy() {
			assertEquals(invocationCount, 1);
		}
	}

	@Test
	void multipleTestTemplatesShouldBeRun() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(MultipleTestTemplates.class);
		assertThat(eventRecorder.getTestSuccessfulCount()).isEqualTo(4);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class MultipleTestTemplates {
		private static int invocationCount = 0;

		public MultipleTestTemplates(int a) {

		}

		@Parameters
		public static Collection<Object[]> data() {
			invocationCount++;
			assertEquals(1, invocationCount);
			return Arrays.asList(new Object[][] { { 3 }, { 4 } });
		}

		@TestTemplate
		void firstTemplate() {

		}

		@TestTemplate
		void secondTemplate() {

		}
	}

	@Test
	void singleParameterIterable() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(IterableTest.class);
		assertThat(eventRecorder.getTestSuccessfulCount()).isEqualTo(2);
	}

	@ExtendWith(ParameterizedExtension.class)
	private static class IterableTest {

		public IterableTest(Object o) {

		}

		@Parameters
		public static Object[] data() {
			return new Object[] { new Object(), new Object() };
		}

		@TestTemplate
		void dummy() {

		}
	}

	private ExecutionEventRecorder executeTestsForClass(Class<?> testClass) {
		LauncherDiscoveryRequest request = request().selectors(selectClass(testClass)).build();
		JupiterTestEngine engine = new JupiterTestEngine();
		TestDescriptor testDescriptor = engine.discover(request, UniqueId.forEngine(engine.getId()));
		ExecutionEventRecorder eventRecorder = new ExecutionEventRecorder();
		engine.execute(new ExecutionRequest(testDescriptor, eventRecorder, request.getConfigurationParameters()));
		return eventRecorder;
	}

	private static List<Throwable> exceptionsThrown(ExecutionEventRecorder executionEventRecorder) {
		return executionEventRecorder.getFailedTestFinishedEvents().stream().map(
			it -> it.getPayload(TestExecutionResult.class)).map(
				o -> o.flatMap(TestExecutionResult::getThrowable)).filter(Optional::isPresent).map(
					Optional::get).collect(Collectors.toList());
	}
}