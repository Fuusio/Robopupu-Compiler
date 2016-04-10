package com.robopupu.api.dependency;

/*
 * {@link DependencyProvider} is an abstract base class for code generated concrete implementations
 * that generated by an annotation processor for {@link DependencyScope} classes annotated with
 * {@link Scope},
 */
public abstract class DependencyProvider {

    public static final String SUFFIX = "_DependencyProvider";

    protected DependencyScope mScope;

    protected abstract <T> T getDependency(final Class<T> dependencyType);

    public void setScope(final DependencyScope scope) {
        mScope = scope;
    }
}