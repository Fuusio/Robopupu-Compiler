package com.robopupu.tests;

import com.robopupu.api.dependency.DependencyProvider;
import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.dependency.Scope;
import com.robopupu.api.plugin.Plugin;

/**
 * {@link FooFeatureScope} tests code generation for a  {@link DependencyProvider} implementation
 * in case there are no provider methods nor constructors assigned for a {@link DependencyScope}.
 */
@Plugin
@Scope
public class FooFeatureScope extends DependencyScope {
}
