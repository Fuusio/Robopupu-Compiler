package com.robopupu.samples;

import com.robopupu.api.dependency.Provides;
import com.robopupu.api.dependency.Scope;

@Scope(DummyDependencyScope.class)
@Provides(Fii.class)
public class FiiImpl implements Fii {
}