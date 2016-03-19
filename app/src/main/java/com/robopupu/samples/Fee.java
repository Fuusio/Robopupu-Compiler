package com.robopupu.samples;

import com.robopupu.api.dependency.Provides;
import com.robopupu.api.dependency.Scope;

/**
 * {@link Fee} ...
 */
public class Fee {

    @Scope(DummyDependencyScope.class)
    @Provides
    public Fee() {
    }
}
