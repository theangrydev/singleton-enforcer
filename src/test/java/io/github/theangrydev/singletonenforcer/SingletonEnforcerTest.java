package io.github.theangrydev.singletonenforcer;

import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class SingletonEnforcerTest implements WithAssertions {

    @Test
    public void doesNotFailWhenASingletonIsConstructedOnlyOnce() {
        SingletonEnforcer singletonEnforcer = new SingletonEnforcer();
        singletonEnforcer.checkSingletons(Singleton::new, Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void failsWhenASingletonIsConstructedMoreThanOnce() {
        SingletonEnforcer singletonEnforcer = new SingletonEnforcer();
        singletonEnforcer.checkSingletons(() -> {
            new Singleton();
            new Singleton();
        }, Singleton.class);
    }
}
