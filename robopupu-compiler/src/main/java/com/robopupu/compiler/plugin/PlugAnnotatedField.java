package com.robopupu.compiler.plugin;

import javax.lang.model.type.TypeMirror;

/**
 * {@link PlugAnnotatedField} is a model class used for storing information about instance fields
 * annotated with {@link com.robopupu.api.plugin.Plug}.
 */
public class PlugAnnotatedField {

    private final TypeMirror mFieldType;
    private final String mScopeClass;

    public PlugAnnotatedField(final TypeMirror fieldType, final String scopeClass) {
        mFieldType = fieldType;
        mScopeClass = scopeClass;
    }

    public TypeMirror getFieldType() {
        return mFieldType;
    }

    public String getScopeClass() {
        return mScopeClass;
    }
}
