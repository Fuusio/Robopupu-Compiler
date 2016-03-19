package com.robopupu.samples;

import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.dependency.Provides;
import com.robopupu.api.dependency.Scope;

/**
 * {@link DummyDependencyScope} ...
 */
@Scope
public class DummyDependencyScope extends DependencyScope {

    @Override
    protected <T> T getDependency() {
        return null;
    }

    @Provides
    public Foo getFoo() {
        return new FooImpl();
    }

}
