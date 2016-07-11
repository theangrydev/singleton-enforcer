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
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class ConstructionCounter {

    private final String packageToCover;

    private Map<Class<?>, List<Object>> classDependencies = new ConcurrentHashMap<>();
    private Map<Object, List<Class<?>>> dependencyUsage = new ConcurrentHashMap<>();

    private Set<Object> seen =  Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Map<Class<?>, AtomicLong> constructionCounts = new ConcurrentHashMap<>();
    private ClassFileTransformer classFileTransformer;
    private Instrumentation instrumentation;

    public ConstructionCounter(String packageToCover) {
        this.packageToCover = packageToCover;
    }

    public void listenForConstructions() {
        Junction<TypeDescription> typeConditions = not(isInterface()).and(not(isSynthetic())).and(nameStartsWith(packageToCover));
        Junction<MethodDescription> constructorConditions = not(isBridge()).and(not(isSynthetic()));

        instrumentation = ByteBuddyAgent.install();
        classFileTransformer = new AgentBuilder.Default().type(typeConditions).transform((builder, typeDescription, classLoader) -> builder
                .constructor(constructorConditions)
                .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(this))))
                .installOn(instrumentation);
    }

    public void stopListeningForConstructions() {
        boolean removed = instrumentation.removeTransformer(classFileTransformer);
        if (!removed) {
            throw new IllegalStateException("Could not remove transformer");
        }
    }

    public Set<Class<?>> classesConstructedMoreThanOnce() {
        return constructionCounts.entrySet().stream()
                .filter(entry -> entry.getValue().longValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    public List<Class<?>> dependencyUsageOutsideOf(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Object> dependencyThatShouldNotBeLeaked = classDependencies.get(singleton).stream()
                .filter(dependency -> typeOfDependencyThatShouldNotBeLeaked.isAssignableFrom(dependency.getClass()))
                .collect(Collectors.toList());
        if (dependencyThatShouldNotBeLeaked.size() != 1) {
            throw new IllegalArgumentException(format("Type '%s' is not a singleton!", singleton));
        }
        return usagesThatAreNotBy(singleton, dependencyUsage.get(dependencyThatShouldNotBeLeaked.get(0)));
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