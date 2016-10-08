package io.github.theangrydev.singletonenforcer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.assertj.core.api.WithAssertions;

public class ConstructionCounterTest implements WithAssertions {

    @Rule
    public final ClearSystemProperties clearPackageToEnforce = new ClearSystemProperties("package.to.enforce");

    @Test
    public void missingPackageToEnforce() {
        assertThatThrownBy(ConstructionCounter::listenForConstructions)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("System property 'package.to.enforce' must be set with the package to enforce!");
    }
}