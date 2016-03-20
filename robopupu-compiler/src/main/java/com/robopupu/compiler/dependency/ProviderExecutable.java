package com.robopupu.compiler.dependency;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * {@link ProviderExecutable} is a model class used for storing information about
 * a constructor annotated with {@link com.robopupu.api.dependency.Provides}.
 */
public abstract class ProviderExecutable {

    protected final ExecutableElement mExecutableElement;
    protected final AnnotationMirror mProvidesAnnotation;

    public ProviderExecutable(final ExecutableElement executableElement, final AnnotationMirror providesAnnotation) {
        mExecutableElement = executableElement;
        mProvidesAnnotation = providesAnnotation;
    }

    public ExecutableElement getExecutableElement() {
        return mExecutableElement;
    }

    public List<? extends VariableElement> getParameters() {
        return mExecutableElement.getParameters();
    }

    public abstract String getProvidedType();

    public boolean hasParameters() {
        return !mExecutableElement.getParameters().isEmpty();
    }
}
