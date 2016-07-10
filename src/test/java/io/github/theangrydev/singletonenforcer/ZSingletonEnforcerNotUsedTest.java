package io.github.theangrydev.singletonenforcer;

import org.junit.Test;

public class ZSingletonEnforcerNotUsedTest {

    @Test
    public void another() {
        new SomeClass();
        System.out.println("true = " + true);
    }
}
