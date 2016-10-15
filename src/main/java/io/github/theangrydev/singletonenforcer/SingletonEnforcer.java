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

import static java.lang.String.format;

/**
 * This is the JUnit {@link org.junit.Rule}.
 */
public final class SingletonEnforcer implements TestRule {

    private static SingletonEnforcer instance;

    private final String packageToEnforce;
    private final ConstructionCounter constructionCounter;

    private SingletonEnforcer(String packageToEnforce, ConstructionCounter constructionCounter) {
        this.packageToEnforce = packageToEnforce;
        this.constructionCounter = constructionCounter;
    }

    /**
     * Factory method for {@link SingletonEnforcer}.
     *
     * @param packageToEnforce The package that contains the classes that assertions will be made about
     * @return The {@link SingletonEnforcer} to use in a JUnit {@link org.junit.Rule}
     */
    public static SingletonEnforcer enforcePackage(String packageToEnforce) {
        if (packageToEnforce == null || packageToEnforce.trim().isEmpty()) {
            throw new IllegalArgumentException("Package to enforce must be provided!");
        }
        synchronized (SingletonEnforcer.class) {
            if (instance == null) {
                ConstructionCounter constructionCounter = new ConstructionCounterFactory().listenForConstructions(packageToEnforce);
                instance = new SingletonEnforcer(packageToEnforce, constructionCounter);
            } else if (!packageToEnforce.equals(instance.packageToEnforce)) {
                throw new IllegalArgumentException(format("SingletonEnforcer can only enforce one package per JVM. The package currently instrumented is '%s' which is different from the given package '%s'", instance.packageToEnforce, packageToEnforce));
            }
            return instance;
        }
    }

    /**
     * @param execution A {@link Runnable} that will, once executed, exercise all the classes that will be asserted about
     * @return A {@link ConstructionCountAssertions} that can assert on the construction counts that were observed
     */
    public ConstructionCountAssertions during(Runnable execution) {
        synchronized (SingletonEnforcer.class) {
            constructionCounter.reset();
            execution.run();
            return new ConstructionCountAssertions(constructionCounter.snapshot());
        }
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return statement;
    }
}