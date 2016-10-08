/*
 * Copyright 2016 Liam Williams <liam.williams@zoho.com>.
 *
 * This file is part of singleton-enforcer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.theangrydev.singletonenforcer;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher.Junction;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.theangrydev.singletonenforcer.SingletonEnforcer.PACKAGE_TO_ENFORCE_SYSTEM_PROPERTY;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.bytebuddy.matcher.ElementMatchers.*;

@SuppressWarnings("PMD.UseConcurrentHashMap") // intentionally using a single global lock
public class ConstructionCounter {

    private static final Object LOCK = new Object();

    private final Map<Class<?>, List<Object>> classDependencies = new HashMap<>();
    private final Map<Object, List<Class<?>>> dependencyUsage = new HashMap<>();

    private final Set<Object> seen =  new HashSet<>();
    private final Map<Class<?>, AtomicLong> constructionCounts = new HashMap<>();

    public static ConstructionCounter listenForConstructions() {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        Junction<TypeDescription> typeConditions = not(isInterface()).and(not(isSynthetic())).and(nameStartsWith(packageToEnforce()));
        Junction<MethodDescription> constructorConditions = not(isBridge()).and(not(isSynthetic()));

        ConstructionCounter constructionCounter = new ConstructionCounter();

        new AgentBuilder.Default().type(typeConditions).transform((builder, typeDescription, classLoader) -> builder
                .constructor(constructorConditions)
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(constructionCounter))))
                .installOn(instrumentation);

        return constructionCounter;
    }

    private static String packageToEnforce() {
        return Optional.ofNullable(System.getProperty(PACKAGE_TO_ENFORCE_SYSTEM_PROPERTY))
                .orElseThrow(() -> new IllegalArgumentException(format("System property '%s' must be set with the package to enforce!", PACKAGE_TO_ENFORCE_SYSTEM_PROPERTY)));
    }

    public void reset() {
        synchronized (LOCK) {
            classDependencies.clear();
            dependencyUsage.clear();
            constructionCounts.clear();
            seen.clear();
        }
    }

    public Set<Class<?>> classesConstructedMoreThanOnce() {
        return constructionCounts.entrySet().stream()
                .filter(entry -> entry.getValue().longValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    public List<Class<?>> dependencyUsageOutsideOf(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Object> dependencies = classDependencies.get(singleton);
        if (dependencies == null || classDependencies.isEmpty()) {
            throw new IllegalStateException(format("Did not see any '%s' constructions at all!", singleton));
        }
        List<Object> dependencyThatShouldNotBeLeaked = dependencies.stream()
                .filter(dependency -> typeOfDependencyThatShouldNotBeLeaked.isAssignableFrom(dependency.getClass()))
                .collect(toList());
        if (dependencyThatShouldNotBeLeaked.isEmpty()) {
            throw new IllegalArgumentException(format("Type '%s' was not constructed with a '%s' at all!", singleton, typeOfDependencyThatShouldNotBeLeaked));
        }
        if (dependencyThatShouldNotBeLeaked.size() > 1) {
            throw new IllegalArgumentException(format("Type '%s' is not a singleton! (it was constructed more than once)", singleton));
        }
        return usagesThatAreNotBy(singleton, dependencyUsage.get(dependencyThatShouldNotBeLeaked.get(0)));
    }

    private List<Class<?>> usagesThatAreNotBy(Class<?> target, List<Class<?>> usages) {
        return usages.stream().filter(aClass -> !aClass.equals(target)).collect(toList());
    }

    @SuppressWarnings("unused") // Invoked by ByteBuddy
    @RuntimeType
    public void intercept(@This Object object, @AllArguments Object... dependencies) {
        synchronized (LOCK) {
            recordDependencies(object.getClass(), dependencies);
            recordUsage(object.getClass(), dependencies);
            boolean alreadySeen = !seen.add(object);
            if (alreadySeen) {
                return;
            }
            AtomicLong atomicLong = constructionCounts.putIfAbsent(object.getClass(), new AtomicLong(1));
            if (atomicLong != null) {
                atomicLong.incrementAndGet();
            }
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // can't help it
    private void recordUsage(Class<?> aClass, Object... dependencies) {
        for (Object dependency : dependencies) {
            List<Class<?>> classes = dependencyUsage.get(dependency);
            if (classes == null) {
                classes = new ArrayList<>();
                dependencyUsage.put(dependency, classes);
            }
            classes.add(aClass);
        }
    }

    private void recordDependencies(Class<?> aClass, Object... dependencies) {
        List<Object> objects = classDependencies.get(aClass);
        if (objects == null) {
            objects = new ArrayList<>();
            classDependencies.put(aClass, objects);
        }
        Collections.addAll(objects, dependencies);
    }
}