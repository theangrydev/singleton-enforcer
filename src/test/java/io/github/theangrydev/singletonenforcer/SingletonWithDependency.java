package io.github.theangrydev.singletonenforcer;

public class SingletonWithDependency {

    public SingletonWithDependency(LeakedDependency dependency, Object other) {
        // nobody else should use my dependency
    }
}
