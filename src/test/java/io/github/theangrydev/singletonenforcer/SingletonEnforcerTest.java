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

public class SingletonEnforcerTest implements WithAssertions {

    @Rule
    public SingletonEnforcer singletonEnforcer = new SingletonEnforcer();

    @Test
    public void doesNotFailWhenASingletonIsConstructedOnlyOnce() {
        new Singleton();
        singletonEnforcer.checkSingletonsAreConstructedOnce(Singleton.class);
    }

    @Test
    public void doesNotFailWhenASingletonWithNestedConstructorIsConstructedOnlyOnce() {
        new SingletonWithNestedConstructor();
        singletonEnforcer.checkSingletonsAreConstructedOnce(SingletonWithNestedConstructor.class);
    }

    @Test
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        new Singleton();
        new Singleton();
        assertThatThrownBy(() -> singletonEnforcer.checkSingletonsAreConstructedOnce(Singleton.class))
                .isInstanceOf(AssertionError.class)
                .hasMessage("The following singletons were constructed more than once: [class io.github.theangrydev.singletonenforcer.Singleton]");
    }

    @Test
    public void leakedDependenciesOnlySupportsActualDependencies() {
        new SomeClass();
        assertThatThrownBy(() -> singletonEnforcer.checkDependencyIsNotLeaked(SomeClass.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class io.github.theangrydev.singletonenforcer.SomeClass' was not constructed with a 'interface io.github.theangrydev.singletonenforcer.LeakedDependencyInterface' at all!");
    }

    @Test
    public void leakedDependenciesOnlySupportsSingletons() {
        new ClassWithLeakedDependency(new LeakedDependency());
        new ClassWithLeakedDependency(new LeakedDependency());
        assertThatThrownBy(() -> singletonEnforcer.checkDependencyIsNotLeaked(ClassWithLeakedDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class io.github.theangrydev.singletonenforcer.ClassWithLeakedDependency' is not a singleton! (it was constructed more than once)");
    }

    @Test
    public void leakedDependenciesOnlySupportsSingletonsThatAreActuallyConstructed() {
        assertThatThrownBy(() -> singletonEnforcer.checkDependencyIsNotLeaked(ClassWithLeakedDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Type 'class io.github.theangrydev.singletonenforcer.ClassWithLeakedDependency' was not constructed with a 'interface io.github.theangrydev.singletonenforcer.LeakedDependencyInterface' at all!");
    }

    @Test
    public void failsWhenDependencyIsLeaked() {
        LeakedDependencyInterface leakedDependency = new LeakedDependency();
        new SingletonWithDependency(leakedDependency, new Object());
        new ClassWithLeakedDependency(leakedDependency);

        assertThatThrownBy(() -> singletonEnforcer.checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class))
                .isInstanceOf(AssertionError.class)
                .hasMessage("The dependency 'interface io.github.theangrydev.singletonenforcer.LeakedDependencyInterface' of 'class io.github.theangrydev.singletonenforcer.SingletonWithDependency' was leaked to: [class io.github.theangrydev.singletonenforcer.ClassWithLeakedDependency]");
    }

    @Test
    public void succeedsWhenDependencyIsNotLeaked() {
        new SingletonWithDependency(new LeakedDependency(), new Object());

        singletonEnforcer.checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class);
    }
}
