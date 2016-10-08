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
    private final Map<Class<?>, AtomicLong> constructionCounts;

    ConstructionCounts(Map<Class<?>, List<Object>> classDependencies, Map<Object, List<Class<?>>> dependencyUsage, Map<Class<?>, AtomicLong> constructionCounts) {
        this.classDependencies = classDependencies;
        this.dependencyUsage = dependencyUsage;
        this.constructionCounts = constructionCounts;
    }

    Set<Class<?>> classesConstructedMoreThanOnce() {
        return constructionCounts.entrySet().stream()
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
