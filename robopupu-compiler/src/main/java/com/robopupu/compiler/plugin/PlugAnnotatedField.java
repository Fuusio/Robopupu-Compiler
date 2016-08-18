package com.robopupu.compiler.plugin;

import javax.lang.model.type.TypeMirror;

/**
 * {@link PlugAnnotatedField} is a model class used for storing information about instance fields
 * annotated with {@link com.robopupu.api.plugin.Plug}.
 */
public class PlugAnnotatedField {

    private final TypeMirror fieldType;
    private final String scopeClass;

    public PlugAnnotatedField(final TypeMirror fieldType, final String scopeClass) {
        this.fieldType = fieldType;
        this.scopeClass = scopeClass;
    }

    public TypeMirror getFieldType() {
        return fieldType;
    }

    public String getScopeClass() {
        return scopeClass;
    }
}
