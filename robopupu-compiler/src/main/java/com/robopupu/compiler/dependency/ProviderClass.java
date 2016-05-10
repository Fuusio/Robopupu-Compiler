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

    protected final TypeElement mTypeElement;
    protected final AnnotationMirror mProvidesAnnotation;

    public ProviderClass(final TypeElement typeElement, final AnnotationMirror providesAnnotation) {
        mTypeElement = typeElement;
        mProvidesAnnotation = providesAnnotation;
    }

    public TypeElement getTypeElement() {
        return mTypeElement;
    }

    public String getDependencyScopeType() {
        return mTypeElement.asType().toString();
    }

    public String getProvidedType() {
        if (mProvidesAnnotation == null) {
            return mTypeElement.toString();
        } else {
            Collection<? extends AnnotationValue> values = mProvidesAnnotation.getElementValues().values();
            final AnnotationValue value = values.iterator().next();
            return value.toString().replace(".class", "");
        }
    }

    public String getType() {
        return mTypeElement.toString();
    }
}
