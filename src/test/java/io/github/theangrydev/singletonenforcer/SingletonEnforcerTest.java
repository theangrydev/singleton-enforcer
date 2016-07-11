package io.github.theangrydev.singletonenforcer;

import org.assertj.core.api.WithAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SingletonEnforcerTest implements WithAssertions {

    private SingletonEnforcer singletonEnforcer = new SingletonEnforcer();

    @Before
    public void setUp() {
        singletonEnforcer.setUp("io.github.theangrydev.singletonenforcer");
    }

    @After
    public void tearDown() {
        singletonEnforcer.tearDown();
    }

    @Test
    public void doesNotFailWhenASingletonIsConstructedOnlyOnce() {
        new Singleton();
        singletonEnforcer.checkSingletons(Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        new Singleton();
        new Singleton();
        singletonEnforcer.checkSingletons(Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void failsWhenDependencyIsLeaked() {
        LeakedDependencyInterface leakedDependency = new LeakedDependency();
        new SingletonWithDependency(leakedDependency, new Object());
        new ClassWithLeakedDependency(leakedDependency);

        singletonEnforcer.checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class);
    }
}
