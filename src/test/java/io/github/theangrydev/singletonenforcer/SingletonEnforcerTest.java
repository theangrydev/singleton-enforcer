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

    @Test(expected = AssertionError.class)
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        new Singleton();
        new Singleton();
        singletonEnforcer.checkSingletonsAreConstructedOnce(Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void failsWhenDependencyIsLeaked() {
        LeakedDependencyInterface leakedDependency = new LeakedDependency();
        new SingletonWithDependency(leakedDependency, new Object());
        new ClassWithLeakedDependency(leakedDependency);

        singletonEnforcer.checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class);
    }
}
