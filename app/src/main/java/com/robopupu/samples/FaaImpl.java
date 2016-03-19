package com.robopupu.samples;

import com.robopupu.api.dependency.Provides;
import com.robopupu.api.dependency.Scope;

/**
 * {@link FaaImpl} ...
 */
public class FaaImpl implements Faa {

    @Scope(DummyDependencyScope.class)
    @Provides(Faa.class)
    public FaaImpl() {
    }
}
