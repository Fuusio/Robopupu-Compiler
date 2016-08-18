package com.robopupu.compiler.dependency;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;

/**
 * {@link ProviderClass} is a model class used for storing information about
 * a class annotated with {@link com.robopupu.api.dependency.Provides}.
 */
public class ProviderClass  {

    protected final TypeElement typeElement;
    protected final AnnotationMirror providesAnnotation;

    public ProviderClass(final TypeElement typeElement, final AnnotationMirror providesAnnotation) {
        this.typeElement = typeElement;
        this.providesAnnotation = providesAnnotation;
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String getDependencyScopeType() {
        return typeElement.asType().toString();
    }

    public String getProvidedType() {
        if (providesAnnotation == null) {
            return typeElement.toString();
        } else {
            Collection<? extends AnnotationValue> values = providesAnnotation.getElementValues().values();
            final AnnotationValue value = values.iterator().next();
            return value.toString().replace(".class", "");
        }
    }

    public String getType() {
        return typeElement.toString();
    }
}
