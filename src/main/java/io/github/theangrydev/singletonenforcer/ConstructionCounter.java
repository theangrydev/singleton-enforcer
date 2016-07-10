package io.github.theangrydev.singletonenforcer;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toSet;
import static net.bytebuddy.matcher.ElementMatchers.any;


public class ConstructionCounter {
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

    @SuppressWarnings("unused") // Invoked by ByteBuddy
    @RuntimeType
    public void intercept(@This Object object) {
        System.out.println("object = " + object);
        if (!seen.add(object)) {
            return;
        }
        AtomicLong atomicLong = constructionCounts.putIfAbsent(object.getClass(), new AtomicLong(1));
        if (atomicLong != null) {
            atomicLong.incrementAndGet();
        }
    }
}
