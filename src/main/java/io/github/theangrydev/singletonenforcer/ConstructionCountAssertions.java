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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * Assertions about the construction counts that were captured during {@link SingletonEnforcer#during(Runnable)}.
 */
public class ConstructionCountAssertions {
    private final ConstructionCounts constructionCounts;

    ConstructionCountAssertions(ConstructionCounts constructionCounts) {
        this.constructionCounts = constructionCounts;
    }

    /**
     * Checks that the given classes were only constructed once.
     *
     * @param singletons The classes to check
     */
    public void checkSingletonsAreConstructedOnce(Class<?>... singletons) {
        checkSingletonsAreConstructedOnce(Arrays.asList(singletons));
    }

    /**
     * Checks that the given classes were only constructed once.
     *
     * @param singletons The classes to check
     */
    public void checkSingletonsAreConstructedOnce(List<Class<?>> singletons) {
        Set<Class<?>> classesConstructedMoreThanOnce = constructionCounts.classesConstructedMoreThanOnce();

        List<Class<?>> notSingletons = new ArrayList<>();
        notSingletons.addAll(singletons);
        notSingletons.retainAll(classesConstructedMoreThanOnce);

        if (!notSingletons.isEmpty()) {
            throw new AssertionError(format("The following singletons were constructed more than once: %s", singletons));
        }
    }

    /**
     * Checks that a constructor dependency of the given singleton class is not "leaked" and passed into the
     * constructor of any other classes.
     *
     * @param singleton                             The class of the singleton to check
     * @param typeOfDependencyThatShouldNotBeLeaked The type of the dependency that is passed into the singleton
     */
    public void checkDependencyIsNotLeaked(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Class<?>> leakedTo = constructionCounts.dependencyUsageOutsideOf(singleton, typeOfDependencyThatShouldNotBeLeaked);
        if (!leakedTo.isEmpty()) {
            throw new AssertionError(format("The dependency '%s' of '%s' was leaked to: %s", typeOfDependencyThatShouldNotBeLeaked, singleton, leakedTo));
        }
    }
}
