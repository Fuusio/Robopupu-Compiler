package com.robopupu.compiler.dependency;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

/**
 * {@link ProviderMethod} is a model class used for storing information about
 * a constructor annotated with {@link com.robopupu.api.dependency.Provides}.
 */
public class ProviderMethod extends ProviderExecutable  {

    public ProviderMethod(final ExecutableElement executableElement, final AnnotationMirror providesAnnotation) {
        super(executableElement, providesAnnotation);
    }

    public String getMethodName() {
        return executableElement.getSimpleName().toString();
    }

    public String getDependencyScopeType() {
        return executableElement.getEnclosingElement().asType().toString();
    }

    @Override
    public String getProvidedType() {
        if (providesAnnotation == null) {
            return executableElement.getReturnType().toString();
        } else {
            Collection<? extends AnnotationValue> values = providesAnnotation.getElementValues().values();
            final AnnotationValue value = values.iterator().next();
            return value.toString().replace(".class", "");
        }
    }

    public String getReturnType() {
        return executableElement.getReturnType().toString();
    }
}
