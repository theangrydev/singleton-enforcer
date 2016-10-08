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

import static io.github.theangrydev.singletonenforcer.ConstructionCounter.listenForConstructions;

@SuppressWarnings("WeakerAccess") // this is part of the public API
public final class SingletonEnforcer implements TestRule {

    public static final String PACKAGE_TO_ENFORCE_SYSTEM_PROPERTY = "package.to.enforce";

    private static final ConstructionCounter CONSTRUCTION_COUNTER = listenForConstructions();

    public SingletonEnforcerAssertions during(Runnable execution) {
        execution.run();
        return new SingletonEnforcerAssertions(CONSTRUCTION_COUNTER);
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        CONSTRUCTION_COUNTER.reset();
        return statement;
    }
}