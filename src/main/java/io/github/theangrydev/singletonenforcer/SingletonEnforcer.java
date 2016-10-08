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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.theangrydev.singletonenforcer.ConstructionCounter.listenForConstructions;
import static java.lang.String.format;
import static org.junit.Assert.fail;

public final class SingletonEnforcer implements TestRule {

    public static final String PACKAGE_TO_ENFORCE_SYSTEM_PROPERTY = "package.to.enforce";

    private static final ConstructionCounter CONSTRUCTION_COUNTER = listenForConstructions();

    public void checkSingletonsAreConstructedOnce(Class<?>... singletons) {
        checkSingletonsAreConstructedOnce(Arrays.asList(singletons));
    }

    public void checkSingletonsAreConstructedOnce(List<Class<?>> singletons) {
        Set<Class<?>> classesConstructedMoreThanOnce = CONSTRUCTION_COUNTER.classesConstructedMoreThanOnce();

        List<Class<?>> notSingletons = new ArrayList<>();
        notSingletons.addAll(singletons);
        notSingletons.retainAll(classesConstructedMoreThanOnce);

        if (!notSingletons.isEmpty()) {
            throw new AssertionError(format("The following singletons were constructed more than once: %s", singletons));
        }
    }

    public void checkDependencyIsNotLeaked(Class<?> singleton, Class<?> typeOfDependencyThatShouldNotBeLeaked) {
        List<Class<?>> leakedTo = CONSTRUCTION_COUNTER.dependencyUsageOutsideOf(singleton, typeOfDependencyThatShouldNotBeLeaked);
        if (!leakedTo.isEmpty()) {
            throw new AssertionError(format("The dependency '%s' of '%s' was leaked to: %s", typeOfDependencyThatShouldNotBeLeaked, singleton, leakedTo));
        }
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        CONSTRUCTION_COUNTER.reset();
        return statement;
    }
}