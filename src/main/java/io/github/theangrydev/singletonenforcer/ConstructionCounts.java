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

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

class ConstructionCounts {

    private final Map<Class<?>, List<Object>> classDependencies;
    private final Map<Object, List<Class<?>>> dependencyUsage;
    private final Map<Class<?>, AtomicLong> timesConstructed;

    ConstructionCounts(Map<Class<?>, List<Object>> classDependencies, Map<Object, List<Class<?>>> dependencyUsage, Map<Class<?>, AtomicLong> timesConstructed) {
        this.classDependencies = classDependencies;
        this.dependencyUsage = dependencyUsage;
        this.timesConstructed = timesConstructed;
    }

    Set<Class<?>> classesConstructedMoreThanOnce() {
        return timesConstructed.entrySet().stream()
                .filter(entry -> entry.getValue().longValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    List<Class<?>> dependencyUsageOutsideOf(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Object> dependencyThatShouldNotBeLeaked = classDependencies.getOrDefault(singleton, emptyList()).stream()
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
}
