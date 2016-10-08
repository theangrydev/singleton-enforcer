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

import example.*;
import org.assertj.core.api.WithAssertions;
import org.junit.Rule;
import org.junit.Test;

import static io.github.theangrydev.singletonenforcer.SingletonEnforcer.enforcePackage;

public class SingletonEnforcerTest implements WithAssertions {

    @Rule
    public SingletonEnforcer singletonEnforcer = enforcePackage("example");

    @Test
    public void nullPackageToEnforce() {
        assertThatThrownBy(() -> enforcePackage(null)).hasMessage("Package to enforce must be provided!");
    }

    @Test
    public void blankPackageToEnforce() {
        assertThatThrownBy(() -> enforcePackage("  ")).hasMessage("Package to enforce must be provided!");
    }

    @Test
    public void doesNotFailWhenASingletonIsConstructedOnlyOnce() {
        singletonEnforcer.during(Singleton::new).checkSingletonsAreConstructedOnce(Singleton.class);
    }

    @Test
    public void doesNotFailWhenASingletonWithNestedConstructorIsConstructedOnlyOnce() {
        singletonEnforcer.during(SingletonWithNestedConstructor::new).checkSingletonsAreConstructedOnce(SingletonWithNestedConstructor.class);
    }

    @Test
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        assertThatThrownBy(() -> singletonEnforcer.during(() -> {
            new Singleton();
            new Singleton();
        }).checkSingletonsAreConstructedOnce(Singleton.class))
                .isInstanceOf(AssertionError.class)
                .hasMessage("The following singletons were constructed more than once: [class example.Singleton]");
    }

    @Test
    public void leakedDependenciesOnlySupportsActualDependencies() {
        assertThatThrownBy(() -> singletonEnforcer.during(Singleton::new).checkDependencyIsNotLeaked(SomeClass.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class example.SomeClass' was not constructed with a 'interface example.LeakedDependencyInterface' at all!");
    }

    @Test
    public void leakedDependenciesOnlySupportsSingletons() {
        assertThatThrownBy(() -> singletonEnforcer.during(() -> {
            new ClassWithLeakedDependency(new LeakedDependency());
            new ClassWithLeakedDependency(new LeakedDependency());
        }).checkDependencyIsNotLeaked(ClassWithLeakedDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class example.ClassWithLeakedDependency' is not a singleton! (it was constructed more than once)");
    }

    @Test
    public void leakedDependenciesOnlySupportsSingletonsThatAreActuallyConstructed() {
        assertThatThrownBy(() -> singletonEnforcer.during(doNothing()).checkDependencyIsNotLeaked(ClassWithLeakedDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class example.ClassWithLeakedDependency' was not constructed with a 'interface example.LeakedDependencyInterface' at all!");
    }

    @Test
    public void failsWhenDependencyIsLeaked() {
        assertThatThrownBy(() -> singletonEnforcer.during(() -> {
            LeakedDependencyInterface leakedDependency = new LeakedDependency();
            new SingletonWithDependency(leakedDependency, new Object());
            new ClassWithLeakedDependency(leakedDependency);
        }).checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(AssertionError.class)
                .hasMessage("The dependency 'interface example.LeakedDependencyInterface' of 'class example.SingletonWithDependency' was leaked to: [class example.ClassWithLeakedDependency]");
    }

    @Test
    public void succeedsWhenDependencyIsNotLeaked() {
        singletonEnforcer.during(() -> {
            new SingletonWithDependency(new LeakedDependency(), new Object());
        }).checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class);
    }

    @Test
    public void shouldNotAllowInstrumentedClassesToBeUsedOutsideOfTheFramework() {
        assertThatThrownBy(SomeClass::new)
                .hasMessage("Instrumented class 'class example.SomeClass' was constructed outside of the SingletonEnforcer! " +
                        "You should use SingletonEnforcer.during to exercise the code you want to assert on. " +
                        "Make sure that you run SingletonEnforcer in a separate JVM so that instrumented classes are only used by SingletonEnforcer!");
    }

    private Runnable doNothing() {
        return () -> {};
    }
}
