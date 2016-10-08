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

import org.assertj.core.api.WithAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class PackageToEnforceUsedBeforeInstrumentingTest implements WithAssertions {

    @Rule
    public final ProvideSystemProperty setPackageToEnforce = new ProvideSystemProperty("package.to.enforce", "io.github.theangrydev.singletonenforcer");

    @Test
    public void missingPackageToEnforce() {
        assertThatThrownBy(ConstructionCounter::listenForConstructions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Found some already loaded classes in the package to enforce 'io.github.theangrydev.singletonenforcer'. SingletonEnforcer must be run in a separate JVM and must be constructed before any classes in that package are loaded! Already loaded classes:")
                .hasMessageContaining(PackageToEnforceUsedBeforeInstrumentingTest.class.getName());
    }
}