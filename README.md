[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.theangrydev/singleton-enforcer/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.github.theangrydev/singleton-enforcer)
[![Build Status](https://travis-ci.org/theangrydev/singleton-enforcer.svg?branch=master)](https://travis-ci.org/theangrydev/singleton-enforcer)
[![codecov](https://codecov.io/gh/theangrydev/singleton-enforcer/branch/master/graph/badge.svg)](https://codecov.io/gh/theangrydev/singleton-enforcer)
[![Javadoc](http://javadoc-badge.appspot.com/io.github.theangrydev/singleton-enforcer.svg?label=javadoc)](http://javadoc-badge.appspot.com/io.github.theangrydev/singleton-enforcer)
[![Gitter](https://badges.gitter.im/singleton-enforcer/Lobby.svg)](https://gitter.im/singleton-enforcer/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# singleton-enforcer
Tool to enforce that certain classes are ony ever constructed once

```xml
<dependency>
    <groupId>io.github.theangrydev</groupId>
    <artifactId>singleton-enforcer</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Releases

### 2.0.0
* Turned `SingletonEnforcer` into a JUnit `@Rule`
* Added a method `SingletonEnforcer.during` that takes a `Runnable` that should execute some code that the assertions will be made about
* This release requires a new system property `package.to.enforce` to be set. This is needed as a system property because `SingletonEnforcer` is implemented as a static singleton, since the instrumentation must take place only once per JVM
* Added a checks to make sure that the classes in the `package.to.enforce` are instrumented before use
* Added a check to make sure that instrumented classes are not used outside of `SingletonEnforcer.during`

### 1.0.1
* Tool to enforce that certain classes are ony ever constructed once
* `SingletonEnforcer` must be `setUp` before use with the package to cover and `tearDown` must be called after use
* `SingletonEnforcer.checkSingletons` checks that the given classes were only constructed once 
* `SingletonEnforcer.checkSingletonDependenciesAreNotLeaked` checks that a dependency of a singleton is only used inside that singleton and not passed on to other objects
