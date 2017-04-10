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

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.commons.meta.API.Usage.Experimental;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.runners.Parameterized;

@API(Experimental)
public class ParameterizedExtension implements TestTemplateInvocationContextProvider {
	private static ExtensionContext.Namespace parameters = ExtensionContext.Namespace.create(
		ParameterizedExtension.class);
	private int parametersCollectionIndex = 0;

	/**
	 * Indicate whether we can provide parameterized support.
	 * This requires the testClass to either have a static {@code @Parameters} method
	 * and correct {@code @Parameter} and their corresponding values
	 * or to have a constructor that could be injected.
	 */
	@Override
	public boolean supports(ContainerExtensionContext context) {
		return hasParametersMethod(context) && (hasCorrectParameterFields(context) != hasArgsConstructor(context));
	}

	@Override
	public Stream<TestTemplateInvocationContext> provide(ContainerExtensionContext context) {
		// grabbing the parent ensures the paremeters are stored in the same store.
		return context.getParent().flatMap(ParameterizedExtension::parameters).map(
				o -> testTemplateContextsFromParameters(o, context)).orElse(Stream.empty());
	}

	private static boolean hasCorrectParameterFields(ExtensionContext context) {
		List<Field> fields = parametersFields(context);

		return !fields.isEmpty() && areParametersFormedCorrectly(fields);
	}

	private static boolean areParametersFormedCorrectly(List<Field> fields) {
		List<Integer> parameterValues = parameterIndexes(fields);
		List<Integer> duplicateIndexes = duplicatedIndexes(parameterValues);
		boolean hasAllIndexes = indexRangeComplete(parameterValues);

		return hasAllIndexes && duplicateIndexes.isEmpty();
	}

	private static List<Integer> parameterIndexes(List<Field> fields) {
		// @formatter:off
		return fields.stream()
				.map(f -> f.getAnnotation(Parameterized.Parameter.class))
				.map(Parameterized.Parameter::value)
				.collect(toList());
		// @formatter:on
	}

	private static List<Integer> duplicatedIndexes(List<Integer> parameterValues) {
		// @formatter:off
		return parameterValues.stream().collect(groupingBy(identity())).entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(toList());
		// @formatter:on
	}

	private static Boolean indexRangeComplete(List<Integer> parameterValues) {
		// @formatter:off
		return parameterValues.stream()
				.max(Integer::compareTo)
				.map(i -> parameterValues.containsAll(IntStream.range(0, i).boxed().collect(toList())))
				.orElse(false);
		// @formatter:on
	}

	private static Optional<Collection<Object[]>> parameters(ExtensionContext context) {
		return context.getStore(parameters).getOrComputeIfAbsent("parameterMethod",
			k -> new ParameterWrapper(callParameters(context)), ParameterWrapper.class).getValue();

	}

	private static Optional<Collection<Object[]>> callParameters(ExtensionContext context) {
		// @formatter:off
		return findParametersMethod(context)
				.map(m -> ReflectionUtils.invokeMethod(m, null))
				.map(ParameterizedExtension::convertParametersMethodReturnType);
		// @formatter:on
	}

	private static boolean hasParametersMethod(ExtensionContext context) {
		return findParametersMethod(context).isPresent();
	}

	private static Optional<Method> findParametersMethod(ExtensionContext extensionContext) {
		// @formatter:off
		return extensionContext.getTestClass()
				.flatMap(ParameterizedExtension::ensureSingleParametersMethod)
				.filter(ReflectionUtils::isPublic);
		// @formatter:on
	}

	private static Optional<Method> ensureSingleParametersMethod(Class<?> testClass) {
		return ReflectionUtils.findMethods(testClass,
			m -> m.isAnnotationPresent(Parameterized.Parameters.class)).stream().findFirst();
	}

	private static Stream<TestTemplateInvocationContext> testTemplateContextsFromParameters(Collection<Object[]> o, ExtensionContext context) {
		if(hasArgsConstructor(context)) {
			return o.stream().map(ParameterizedExtension::parameterResolver);
		} else if (hasCorrectParameterFields(context)) {
			return o.stream().map(ParameterizedExtension::contextFactory);
		}

		return Stream.empty();
	}

	private static TestTemplateInvocationContext parameterResolver(Object[] objects) {
		return new TestTemplateInvocationContext() {
			@Override
			public List<Extension> getAdditionalExtensions() {
				return singletonList(new ParameterResolver() {
					@Override
					public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
						final Executable declaringExecutable = parameterContext.getDeclaringExecutable();
						return declaringExecutable instanceof Constructor && declaringExecutable.getParameterCount() == objects.length;
					}

					@Override
					public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
						return objects[parameterContext.getIndex()];
					}
				});
			}
		};
	}

	private static TestTemplateInvocationContext contextFactory(Object[] parameters) {
		return new TestTemplateInvocationContext() {
			@Override
			public List<Extension> getAdditionalExtensions() {
				return singletonList(new InjectionExtension(parameters));
			}
		};
	}

	private static class InjectionExtension implements TestInstancePostProcessor {
		private final Object[] parameters;

		public InjectionExtension(Object[] parameters) {
			this.parameters = parameters;
		}

		@Override
		public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
			List<Field> parameters = parametersFields(context);

			if (!parameters.isEmpty() && parameters.size() != this.parameters.length) {
				throw unMatchedAmountOfParametersException();
			}

			for (Field param : parameters) {
				Parameterized.Parameter annotation = param.getAnnotation(Parameterized.Parameter.class);
				int paramIndex = annotation.value();
				param.set(testInstance, this.parameters[paramIndex]);
			}
		}
	}

	private static boolean hasArgsConstructor(ExtensionContext context) {
		// @formatter:off
		return context.getTestClass()
				.map(ReflectionUtils::getDeclaredConstructor)
//				.filter(c -> c.getParameterCount() > 0)
				.isPresent();
		// @formatter:on
	}

	private static List<Field> parametersFields(ExtensionContext context) {
		// @formatter:off
		Stream<Field> fieldStream = context.getTestClass()
				.map(Class::getDeclaredFields)
				.map(Stream::of)
				.orElse(Stream.empty());
		// @formatter:on

		return fieldStream.filter(f -> f.isAnnotationPresent(Parameterized.Parameter.class)).filter(
			ReflectionUtils::isPublic).collect(toList());
	}

	private static ParameterResolutionException unMatchedAmountOfParametersException() {
		return new ParameterResolutionException(
			"The amount of parametersFields in the constructor doesn't match those in the provided parametersFields");
	}

	private static ParameterResolutionException wrongParametersReturnType() {
		return new ParameterResolutionException("The @Parameters returns the wrong type");
	}

	@SuppressWarnings("unchecked")
	private static Collection<Object[]> convertParametersMethodReturnType(Object o) {
		if (o instanceof Collection) {
			return (Collection<Object[]>) o;
		}
		else {
			throw wrongParametersReturnType();
		}
	}

	private static class ParameterWrapper {
		private final Optional<Collection<Object[]>> value;

		public ParameterWrapper(Optional<Collection<Object[]>> value) {
			this.value = value;
		}

		public Optional<Collection<Object[]>> getValue() {
			return value;
		}
	}
}
