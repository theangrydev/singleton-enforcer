package io.github.theangrydev.singletonenforcer;

public class SingletonWithDependency {

    public SingletonWithDependency(LeakedDependencyInterface dependency, Object other) {
        // nobody else should use my dependency
    }
}
