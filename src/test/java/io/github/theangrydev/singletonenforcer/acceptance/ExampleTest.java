package io.github.theangrydev.singletonenforcer.acceptance;

import example.*;
import io.github.theangrydev.singletonenforcer.SingletonEnforcer;
import org.junit.Rule;
import org.junit.Test;

public class ExampleTest {

    @Rule
    public SingletonEnforcer singletonEnforcer = SingletonEnforcer.enforcePackage("example");

    @Test(expected = AssertionError.class)
    public void singletonConstructedTwiceWillThrowException() {
        singletonEnforcer.during(() -> {
            new Singleton();
            new Singleton();
        }).checkSingletonsAreConstructedOnce(Singleton.class);
    }

    @Test(expected = AssertionError.class)
    public void leakedDependencyWillThrowAssertionError() {
        singletonEnforcer.during(() -> {
            LeakedDependencyInterface leakedDependency = new LeakedDependency();
            new SingletonWithDependency(leakedDependency, new Object());
            new ClassWithLeakedDependency(leakedDependency);
        }).checkDependencyIsNotLeaked(SingletonWithDependency.class, LeakedDependencyInterface.class);
    }
}
