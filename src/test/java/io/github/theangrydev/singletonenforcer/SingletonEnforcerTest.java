package io.github.theangrydev.singletonenforcer;

import org.assertj.core.api.WithAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SingletonEnforcerTest implements WithAssertions {

    private SingletonEnforcer singletonEnforcer = new SingletonEnforcer();

    @Before
    public void setUp() {
        singletonEnforcer.setUp();
    }

    @After
    public void tearDown() {
        singletonEnforcer.tearDown();
    }

    @Test
    public void doesNotFailWhenASingletonIsConstructedOnlyOnce() {
        singletonEnforcer.checkSingletons(Singleton::new, Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        singletonEnforcer.checkSingletons(() -> {
            new Singleton();
            new Singleton();
        }, Singleton.class);
    }
}
