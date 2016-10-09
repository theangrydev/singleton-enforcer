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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * This class is not part of the public API.
 * <p>
 * This is the class that counts all the constructions it sees up by instrumenting all classes in the target package.
 */
@SuppressWarnings({
        "PMD.UseConcurrentHashMap", // intentionally using a single global lock
        "WeakerAccess", // used by ByteBuddy; must be public
        "PMD.TooManyMethods" // TODO: see what can be refactored
})
public final class ConstructionCounter extends SecurityManager {

    private final Map<Class<?>, List<Object>> classDependencies = new HashMap<>();
    private final Map<Object, List<Class<?>>> dependencyUsage = new HashMap<>();

    private final Set<Object> seen = new HashSet<>();
    private final Map<Class<?>, AtomicLong> timesConstructed = new HashMap<>();

    private ConstructionCounter() {
        // should only be constructed via the static factory method
    }

    static ConstructionCounter listenForConstructions(String packageToEnforce) {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        checkThatClassesInThePackageToEnforceAreNotAlreadyLoaded(packageToEnforce, instrumentation);

        ConstructionCounter constructionCounter = new ConstructionCounter();

        instrument(packageToEnforce, instrumentation, constructionCounter);

        return constructionCounter;
    }

    /**
     * This class is not part of the public API.
     * <p>
     * This is the instrumentation method that captures all constructor calls.
     *
     * @param object       The "this" object
     * @param dependencies The arguments passed to the constructor
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // used by ByteBuddy; must be public
    @RuntimeType
    public void intercept(@This Object object, @AllArguments Object... dependencies) {
        checkConstructionWasBySingletonEnforcer(object);

        synchronized (ConstructionCounter.class) {
            recordDependencies(object.getClass(), dependencies);
            recordUsage(object.getClass(), dependencies);
            boolean alreadySeen = !seen.add(object);
            if (alreadySeen) {
                return;
            }
            AtomicLong atomicLong = timesConstructed.putIfAbsent(object.getClass(), new AtomicLong(1));
            if (atomicLong != null) {
                atomicLong.incrementAndGet();
            }
        }
    }

    ConstructionCounts snapshot() {
        return new ConstructionCounts(new HashMap<>(classDependencies), new HashMap<>(dependencyUsage), new HashMap<>(timesConstructed));
    }

    void reset() {
        synchronized (ConstructionCounter.class) {
            classDependencies.clear();
            dependencyUsage.clear();
            timesConstructed.clear();
            seen.clear();
        }
    }

    private static void checkThatClassesInThePackageToEnforceAreNotAlreadyLoaded(String packageToEnforce, Instrumentation instrumentation) {
        List<Class> alreadyLoaded = Arrays.stream(instrumentation.getAllLoadedClasses())
                .filter(aClass -> !aClass.isSynthetic())
                .filter(aClass -> !aClass.isInterface())
                .filter(aClass -> startsWith(packageToEnforce, aClass))
                .collect(toList());

        if (!alreadyLoaded.isEmpty()) {
            throw new IllegalStateException(format("Found some already loaded classes in the package to enforce '%s'. " +
                    "SingletonEnforcer must be run in a separate JVM and must be constructed before any classes in that package are loaded! " +
                    "Already loaded classes:%n%s", packageToEnforce, alreadyLoaded));
        }
    }

    private static void instrument(String packageToEnforce, Instrumentation instrumentation, ConstructionCounter constructionCounter) {
        Junction<TypeDescription> typeConditions = not(isInterface()).and(not(isSynthetic())).and(nameStartsWith(packageToEnforce));
        Junction<MethodDescription> constructorConditions = not(isBridge()).and(not(isSynthetic()));

        new AgentBuilder.Default().type(typeConditions).transform((builder, typeDescription, classLoader) -> builder
                .constructor(constructorConditions)
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(constructionCounter))))
                .installOn(instrumentation);
    }

    private static boolean startsWith(String packageToEnforce, Class<?> aClass) {
        Package aPackage = aClass.getPackage();
        return aPackage != null && (packageIsEqualTo(packageToEnforce, aPackage) || packageIsSubPackageOf(packageToEnforce, aPackage));
    }

    private static boolean packageIsSubPackageOf(String packageToEnforce, Package aPackage) {
        return aPackage.getName().matches(packageToEnforce + "\\..*");
    }

    private static boolean packageIsEqualTo(String packageToEnforce, Package aPackage) {
        return aPackage.getName().equals(packageToEnforce);
    }

    private void checkConstructionWasBySingletonEnforcer(@This Object object) {
        if (!calledBySingletonEnforcer()) {
            throw new IllegalStateException(format("Instrumented class '%s' was constructed outside of the SingletonEnforcer! " +
                    "You should use SingletonEnforcer.during to exercise the code you want to assert on. " +
                    "Make sure that you run SingletonEnforcer in a separate JVM so that instrumented classes are only used by SingletonEnforcer!", object.getClass()));
        }
    }

    private boolean calledBySingletonEnforcer() {
        return Arrays.stream(getClassContext()).filter(aClass -> aClass.equals(SingletonEnforcer.class)).count() > 0;
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