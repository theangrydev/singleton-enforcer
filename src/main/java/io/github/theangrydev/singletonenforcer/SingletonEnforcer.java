package io.github.theangrydev.singletonenforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.fail;

public class SingletonEnforcer {

    private final ConstructionCounter constructionCounter;

    public SingletonEnforcer() {
        constructionCounter = new ConstructionCounter();
        constructionCounter.listenForConstructions();
    }

    public void checkSingletons(Runnable execution, Class<?>... singletons) {
        checkSingletons(execution, Arrays.asList(singletons));
    }

    private void checkSingletons(Runnable execution, List<Class<?>> singletons) {
        execution.run();

        Set<Class<?>> classesConstructedMoreThanOnce = constructionCounter.classesConstructedMoreThanOnce();

        List<Class<?>> notSingletons = new ArrayList<>();
        notSingletons.addAll(singletons);
        notSingletons.retainAll(classesConstructedMoreThanOnce);

        if (!notSingletons.isEmpty()) {
            fail(format("The following singletons were constructed more than once: %s", singletons));
        }
    }
}
