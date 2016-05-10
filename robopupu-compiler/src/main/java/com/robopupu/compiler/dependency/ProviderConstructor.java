package com.robopupu.compiler.dependency;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

/**
 * {@link ProviderConstructor} is a model class used for storing information about
 * a constructor annotated with {@link com.robopupu.api.dependency.Provides}.
 */
public class ProviderConstructor extends ProviderExecutable {

    public ProviderConstructor(final ExecutableElement executableElement, final AnnotationMirror providesAnnotation) {
        super(executableElement, providesAnnotation);
    }

    public String getConstructorName() {
        return mExecutableElement.getEnclosingElement().asType().toString();
    }

    @Override
    public String getProvidedType() {
        if (mProvidesAnnotation == null) {
            return mExecutableElement.getEnclosingElement().asType().toString();
        } else {
            Collection<? extends AnnotationValue> values = mProvidesAnnotation.getElementValues().values();
            final AnnotationValue value = values.iterator().next();
            return value.toString().replace(".class", "");
        }
    }

    public String getType() {
        return mExecutableElement.getEnclosingElement().asType().toString();
    }
}
