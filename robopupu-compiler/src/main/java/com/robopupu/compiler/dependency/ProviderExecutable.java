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

    protected final ExecutableElement executableElement;
    protected final AnnotationMirror providesAnnotation;

    public ProviderExecutable(final ExecutableElement executableElement, final AnnotationMirror providesAnnotation) {
        this.executableElement = executableElement;
        this.providesAnnotation = providesAnnotation;
    }

    public ExecutableElement getExecutableElement() {
        return executableElement;
    }

    public List<? extends VariableElement> getParameters() {
        return executableElement.getParameters();
    }

    public abstract String getProvidedType();

    public boolean hasParameters() {
        return !executableElement.getParameters().isEmpty();
    }
}
