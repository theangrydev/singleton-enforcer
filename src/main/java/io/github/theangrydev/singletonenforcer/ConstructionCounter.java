package io.github.theangrydev.singletonenforcer;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.bytebuddy.matcher.ElementMatchers.any;


public class ConstructionCounter {

    private Map<Class<?>, List<Object>> classDependencies = new ConcurrentHashMap<>();
    private Map<Object, List<Class<?>>> dependencyUsage = new ConcurrentHashMap<>();

    private Set<Object> seen =  Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Map<Class<?>, AtomicLong> constructionCounts = new ConcurrentHashMap<>();
    private ClassFileTransformer classFileTransformer;
    private Instrumentation instrumentation;

    public void listenForConstructions() {
        instrumentation = ByteBuddyAgent.install();
        classFileTransformer = new AgentBuilder.Default().type(any()).transform((builder, typeDescription, classLoader) -> builder
                .constructor(any())
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(this))))
                .installOn(instrumentation);
    }

    public void stopListeningForConstructions() {
        instrumentation.removeTransformer(classFileTransformer);
    }

    public Set<Class<?>> classesConstructedMoreThanOnce() {
        return constructionCounts.entrySet().stream()
                .filter(entry -> entry.getValue().longValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    public List<Class<?>> dependencyUsageOutsideOf(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        Object dependencyThatShouldNotBeLeaked = classDependencies.get(singleton).stream()
                .filter(dependency -> typeOfDependencyThatShouldNotBeLeaked.isAssignableFrom(dependency.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(format("Singleton '%s' does not have dependency of type '%s'", singleton, typeOfDependencyThatShouldNotBeLeaked)));
        return usagesThatAreNotBy(singleton, dependencyUsage.get(dependencyThatShouldNotBeLeaked));
    }

    private List<Class<?>> usagesThatAreNotBy(Class<?> target, List<Class<?>> usages) {
        return usages.stream().filter(aClass -> !aClass.equals(target)).collect(toList());
    }

    @SuppressWarnings("unused") // Invoked by ByteBuddy
    @RuntimeType
    public void intercept(@This Object object, @AllArguments Object[] dependencies) {
        recordDependencies(object.getClass(), dependencies);
        recordUsage(object.getClass(), dependencies);
        if (!seen.add(object)) {
            return;
        }
        AtomicLong atomicLong = constructionCounts.putIfAbsent(object.getClass(), new AtomicLong(1));
        if (atomicLong != null) {
            atomicLong.incrementAndGet();
        }
    }

    private void recordUsage(Class<?> aClass, Object[] dependencies) {
        for (Object dependency : dependencies) {
            List<Class<?>> classes = dependencyUsage.get(dependency);
            if (classes == null) {
                classes = new ArrayList<>();
                dependencyUsage.put(dependency, classes);
            }
            classes.add(aClass);
        }
    }

    private void recordDependencies(Class<?> aClass, Object[] dependencies) {
        List<Object> objects = classDependencies.get(aClass);
        if (objects == null) {
            objects = new ArrayList<>();
            classDependencies.put(aClass, objects);
        }
        Collections.addAll(objects, dependencies);
    }
}
