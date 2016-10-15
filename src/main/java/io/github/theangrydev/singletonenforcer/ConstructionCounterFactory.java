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
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static net.bytebuddy.matcher.ElementMatchers.*;

public final class ConstructionCounterFactory {

    ConstructionCounter listenForConstructions(String packageToEnforce) {
        ConstructionCounter constructionCounter = new ConstructionCounter();

        Instrumentation instrumentation = ByteBuddyAgent.install();

        checkThatClassesInThePackageToEnforceAreNotAlreadyLoaded(packageToEnforce, instrumentation);

        ElementMatcher.Junction<TypeDescription> typeConditions = not(isInterface()).and(not(isSynthetic())).and(nameStartsWith(packageToEnforce));
        ElementMatcher.Junction<MethodDescription> constructorConditions = not(isBridge()).and(not(isSynthetic()));

        new AgentBuilder.Default().type(typeConditions).transform((builder, typeDescription, classLoader) -> builder
                .constructor(constructorConditions)
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(constructionCounter))))
                .installOn(instrumentation);

        return constructionCounter;
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
}
