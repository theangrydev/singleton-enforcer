package io.github.theangrydev.singletonenforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.fail;

public class SingletonEnforcer {

    private ConstructionCounter constructionCounter;

    public void setUp(String packageToCover) {
        constructionCounter = new ConstructionCounter(packageToCover);
        constructionCounter.listenForConstructions();
    }

    public void tearDown() {
        constructionCounter.stopListeningForConstructions();
    }

    public void checkSingletons(Class<?>... singletons) {
        checkSingletons(Arrays.asList(singletons));
    }

    public void checkSingletons(List<Class<?>> singletons) {
        checkSingletonsAreConstructedOnce(singletons);
    }

    public void checkDependencyIsNotLeaked(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Class<?>> leakedTo = constructionCounter.dependencyUsageOutsideOf(singleton, typeOfDependencyThatShouldNotBeLeaked);
        if (!leakedTo.isEmpty()) {
            fail(format("The dependency '%s' of '%s' was leaked to: %s", typeOfDependencyThatShouldNotBeLeaked, singleton, leakedTo));
        }
    }

    private void checkSingletonsAreConstructedOnce(List<Class<?>> singletons) {
        Set<Class<?>> classesConstructedMoreThanOnce = constructionCounter.classesConstructedMoreThanOnce();

        List<Class<?>> notSingletons = new ArrayList<>();
        notSingletons.addAll(singletons);
        notSingletons.retainAll(classesConstructedMoreThanOnce);

        if (!notSingletons.isEmpty()) {
            fail(format("The following singletons were constructed more than once: %s", singletons));
        }
    }
}