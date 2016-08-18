package com.robopupu.compiler.fsm;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import com.robopupu.api.fsm.StateEngine;
import com.robopupu.api.fsm.StateMachineEvents;
import com.robopupu.compiler.util.ProcessorException;
import com.robopupu.compiler.util.StringToolkit;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * {@link StateEngineClass} is a model class used for storing information about interface classes annotated
 * with {@link StateMachineEvents}. The class is also used for generate Java code from
 * the annotated interface classes.
 */
public class StateEngineClass {

    private static final String STATE_ENGINE_CLASS_NAME = "State";

    private static final ClassName CLASS_STATE_ENGINE = ClassName.get(StateEngine.class);

    private final String className;
    private final HashMap<String, TypeElement> contextInterfaces;
    private final HashMap<String, TypeElement> eventInterfaces;
    private final HashMap<String, EventMethod> eventMethods;
    private final HashMap<String, SetterMethod> setterMethods;

    public StateEngineClass(final String className) throws ProcessorException {
        this.className = className;
        contextInterfaces = new HashMap<>();
        eventInterfaces = new HashMap<>();
        eventMethods = new HashMap<>();
        setterMethods = new HashMap<>();
    }

    public void addEventMethod(final EventMethod eventMethod) {
        final String signature = eventMethod.getSignature();
        eventMethods.put(signature, eventMethod);
    }

    public void addSetterMethod(final SetterMethod setterMethod) {
        final String signature = setterMethod.getSignature();
        setterMethods.put(signature, setterMethod);
    }

    public void addEventInterface(final TypeElement eventInterface) {
        eventInterfaces.put(eventInterface.getQualifiedName().toString(), eventInterface);
    }

    public void addContextInterface(final TypeElement contextInterface) {
        contextInterfaces.put(contextInterface.getQualifiedName().toString(), contextInterface);
    }

    public void generateCode(final Elements elementUtils, final Filer filer) throws IOException {
        final String packageName  = StringToolkit.getPackageName(className);
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(STATE_ENGINE_CLASS_NAME);
        final ParameterizedTypeName superType = ParameterizedTypeName.get(CLASS_STATE_ENGINE, TypeVariableName.get(STATE_ENGINE_CLASS_NAME));

        classBuilder.superclass(superType);
        classBuilder.addModifiers(Modifier.PUBLIC);

        for (final TypeElement eventInterface : eventInterfaces.values()) {
            classBuilder.addSuperinterface(TypeName.get(eventInterface.asType()));
        }

        for (final TypeElement contextInterface : contextInterfaces.values()) {
            classBuilder.addSuperinterface(TypeName.get(contextInterface.asType()));
        }

        buildPrivateConstructor(classBuilder);
        buildStateConstructor(classBuilder);
        buildCreateMethod(classBuilder);

        for (final EventMethod eventMethod : eventMethods.values()) {
            buildEventDispatcherMethod(classBuilder, eventMethod);
        }

        for (final SetterMethod setterMethod : setterMethods.values()) {
            buildContextGetterMethod(classBuilder, setterMethod);
            buildContextSetterMethod(classBuilder, setterMethod);
        }

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private void buildStateConstructor(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder();
        methodBuilder.addModifiers(Modifier.PROTECTED);

        final TypeName bounds = WildcardTypeName.subtypeOf(TypeVariableName.get(STATE_ENGINE_CLASS_NAME));
        final ParameterizedTypeName type = ParameterizedTypeName.get(ClassName.get(Class.class), bounds);

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, "superStateClass", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        parameterBuilder = ParameterSpec.builder(type, "initialStateClass", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        methodBuilder.addStatement("super(superStateClass, initialStateClass)");
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildPrivateConstructor(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder();
        methodBuilder.addModifiers(Modifier.PRIVATE);

        final TypeName bounds = WildcardTypeName.subtypeOf(TypeVariableName.get(STATE_ENGINE_CLASS_NAME));
        final ParameterizedTypeName type = ParameterizedTypeName.get(ClassName.get(Class.class), bounds);
        final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, "initialStateClass", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        methodBuilder.addStatement("super(initialStateClass)");
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildCreateMethod(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        methodBuilder.returns(TypeVariableName.get(STATE_ENGINE_CLASS_NAME));

        final TypeName bounds = WildcardTypeName.subtypeOf(TypeVariableName.get(STATE_ENGINE_CLASS_NAME));
        final ParameterizedTypeName type = ParameterizedTypeName.get(ClassName.get(Class.class), bounds);
        final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, "initialStateClass", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        methodBuilder.addStatement(String.format("return new %s(initialStateClass)", STATE_ENGINE_CLASS_NAME));
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildEventDispatcherMethod(final TypeSpec.Builder classBuilder, final EventMethod eventMethod) {
        final List<? extends VariableElement> parameters = eventMethod.getParameters();
        final String methodName = eventMethod.getMethodName();
        final String invocation = createInvocation(parameters, methodName);
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);
        methodBuilder.addModifiers(Modifier.PUBLIC);

        for (final VariableElement parameter : parameters) {
            final TypeName typeName = TypeName.get(parameter.asType());
            final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(typeName, parameter.getSimpleName().toString(), Modifier.FINAL);
            methodBuilder.addParameter(parameterBuilder.build());
        }

        methodBuilder.beginControlFlow("if (isStateEngine())");
        methodBuilder.addStatement(String.format("currentState.%s", invocation));
        methodBuilder.nextControlFlow("else if (superState != getStateEngine())");
        methodBuilder.addStatement(String.format("superState.%s", invocation));
        methodBuilder.nextControlFlow("else");
        methodBuilder.addStatement(String.format("onError(this, StateEngine.Error.ERROR_UNHANDLED_EVENT, \"%s\")", methodName));
        methodBuilder.endControlFlow();
        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Create a setter method and a instance field for the context reference.
     * @param classBuilder A {@link TypeSpec.Builder} for adding the created method and field.
     * @param setterMethod A {@link SetterMethod} that contains information to create setter method.
     */
    private void buildContextSetterMethod(final TypeSpec.Builder classBuilder, final SetterMethod setterMethod) {

        // Create a setter method for the context reference

        final List<? extends VariableElement> parameters = setterMethod.getParameters();
        final String methodName = setterMethod.getMethodName();
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);
        final VariableElement parameter = parameters.get(0);
        final TypeName typeName = TypeName.get(parameter.asType());
        final String parameterName = parameter.getSimpleName().toString();
        final String fieldName = parameterName;
        final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(typeName, parameterName, Modifier.FINAL);

        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        methodBuilder.addStatement(String.format("this.%s = %s", fieldName, parameterName));

        // Create an instance field for the context reference

        final FieldSpec.Builder fieldBuilder = FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE);
        classBuilder.addField(fieldBuilder.build());

        classBuilder.addMethod(methodBuilder.build());
    }

    /**
     * Create a getter method for the context reference.
     * @param classBuilder A {@link TypeSpec.Builder} for adding the created method.
     * @param setterMethod A {@link SetterMethod} that contains information to create getter method.
     */
    private void buildContextGetterMethod(final TypeSpec.Builder classBuilder, final SetterMethod setterMethod) {

        final List<? extends VariableElement> parameters = setterMethod.getParameters();
        final VariableElement parameter = parameters.get(0);
        final TypeName typeName = TypeName.get(parameter.asType());
        final String parameterName = parameter.getSimpleName().toString();
        final String formattedName = StringToolkit.upperCaseFirstCharacter(parameterName);
        final String fieldName = parameterName;
        final String methodName = "get" + formattedName;
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);
        methodBuilder.addModifiers(Modifier.PROTECTED, Modifier.FINAL);
        methodBuilder.returns(typeName);
        methodBuilder.beginControlFlow("if (isStateEngine())");
        methodBuilder.addStatement(String.format("return %s", fieldName));
        methodBuilder.nextControlFlow("else");
        methodBuilder.addStatement(String.format("return getStateEngine().%s()", methodName));
        methodBuilder.endControlFlow();

        classBuilder.addMethod(methodBuilder.build());
    }

    private String createInvocation(final List<? extends VariableElement> parameters, final String methodName) {
        final StringBuilder builder = new StringBuilder(methodName);
        builder.append("(");

        if (!parameters.isEmpty()) {
            boolean firstParameter = true;

            for (final VariableElement parameter : parameters) {
                if (firstParameter) {
                    firstParameter = false;
                } else {
                    builder.append(',');
                }
                builder.append(parameter.getSimpleName().toString());
            }
        }

        builder.append(")");
        return builder.toString();
    }
}
