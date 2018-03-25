[![Maven Central](https://img.shields.io/maven-central/v/io.github.theangrydev/singleton-enforcer.svg)](https://mvnrepository.com/artifact/io.github.theangrydev/singleton-enforcer)
[![Javadoc](http://javadoc-badge.appspot.com/io.github.theangrydev/singleton-enforcer.svg?label=javadoc)](http://javadoc-badge.appspot.com/io.github.theangrydev/singleton-enforcer)
[![Gitter](https://badges.gitter.im/singleton-enforcer/Lobby.svg)](https://gitter.im/singleton-enforcer/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

[![Build Status](https://travis-ci.org/theangrydev/singleton-enforcer.svg?branch=master)](https://travis-ci.org/theangrydev/singleton-enforcer)
[![codecov](https://codecov.io/gh/theangrydev/singleton-enforcer/branch/master/graph/badge.svg)](https://codecov.io/gh/theangrydev/singleton-enforcer)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/ea483778bdf04ad899c81afd058299eb)](https://www.codacy.com/app/liam-williams/singleton-enforcer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=theangrydev/singleton-enforcer&amp;utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/f6dc7555-60d8-4429-bf84-471d7c55f132)](https://codebeat.co/projects/github-com-theangrydev-singleton-enforcer)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=io.github.theangrydev:singleton-enforcer)](https://sonarqube.com/dashboard/index/io.github.theangrydev:singleton-enforcer)

# singleton-enforcer
Tool to enforce that certain classes are ony ever constructed once

[Example:](https://github.com/theangrydev/singleton-enforcer/blob/master/src/test/java/acceptance/ExampleTest.java)
```java
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
```

Dependency:
```xml
<dependency>
    <groupId>io.github.theangrydev</groupId>
    <artifactId>singleton-enforcer</artifactId>
    <version>2.1.0</version>
</dependency>
```

## Releases
### 2.1.0
* Made the public API thread safe
* Added documentation for the public API

### 2.0.1
* Turned `SingletonEnforcer` into a JUnit `@Rule`
* Added a method `SingletonEnforcer.during` that takes a `Runnable` that should execute some code that the assertions will be made about
* Added a checks to make sure that the classes in the package to enforce are instrumented before use
* Added a check to make sure that instrumented classes are not used outside of `SingletonEnforcer.during`

### 1.0.1
* Tool to enforce that certain classes are ony ever constructed once
* `SingletonEnforcer` must be `setUp` before use with the package to cover and `tearDown` must be called after use
* `SingletonEnforcer.checkSingletons` checks that the given classes were only constructed once 
* `SingletonEnforcer.checkSingletonDependenciesAreNotLeaked` checks that a dependency of a singleton is only used inside that singleton and not passed on to other objects
