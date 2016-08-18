package com.robopupu.compiler.mvp;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import com.robopupu.api.mvp.OnClick;
import com.robopupu.api.mvp.OnTextChanged;
import com.robopupu.api.mvp.PresenterDelegate;
import com.robopupu.compiler.util.ProcessorException;
import com.robopupu.compiler.util.StringToolkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * {@link EventsDelegateClass} is a model class used for storing information about interface methods
 * annotated  with {@link OnClick} and {@link OnTextChanged}. The class is also used for generate
 * Java code for {@link PresenterDelegate} implementations.
 */
public class EventsDelegateClass {

    protected static final String PREFIX_ON = "on";
    protected static final String POSTFIX_CHECKED = "Checked";
    protected static final String POSTFIX_CLICK = "Click";
    protected static final String POSTFIX_TEXT_CHANGED = "TextChanged";

    private final static String SUFFIX_EVENTS_DELEGATE = "_EventsDelegate";
    private final static ClassName CLASS_PRESENTER_DELEGATE = ClassName.get(PresenterDelegate.class);

    private final HashMap<EventHandlerMethod.EventType, List<EventHandlerMethod>> eventHandlerMethods;
    private TypeElement presenterInterface;

    public EventsDelegateClass(final TypeElement presenterInterface) throws ProcessorException {
        this.presenterInterface = presenterInterface;
        eventHandlerMethods = new HashMap<>();
    }

    public void addEventHandlerMethod(final EventHandlerMethod method) {
        final EventHandlerMethod.EventType eventType = method.getEventType();
        List<EventHandlerMethod> typedMethods = eventHandlerMethods.get(eventType);

        if (typedMethods == null) {
            typedMethods = new ArrayList<>();
            eventHandlerMethods.put(eventType, typedMethods);
        }
        typedMethods.add(method);
    }

    public void generateCode(final Elements elementUtils, final Filer filer) throws IOException {
        final String presenterQualifiedName = presenterInterface.getQualifiedName().toString();
        final String packageName = StringToolkit.getPackageName(presenterQualifiedName);
        final String className = StringToolkit.getClassName(presenterQualifiedName) + SUFFIX_EVENTS_DELEGATE;
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
        final ParameterizedTypeName superType = ParameterizedTypeName.get(CLASS_PRESENTER_DELEGATE, TypeVariableName.get(presenterQualifiedName));

        classBuilder.superclass(superType);
        classBuilder.addModifiers(Modifier.PUBLIC);

        buildPublicConstructor(classBuilder);
        buildOnCheckedMethod(classBuilder);
        buildOnClickMethod(classBuilder);
        buildOnTextChangedMethod(classBuilder);

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private void buildPublicConstructor(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder();
        methodBuilder.addModifiers(Modifier.PUBLIC);

        final String presenterQualifiedName = presenterInterface.getQualifiedName().toString();
        final TypeName type = TypeVariableName.get(presenterQualifiedName);
        final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, "presenter", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());
        methodBuilder.addStatement("super(presenter)");
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildOnCheckedMethod(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(PREFIX_ON + POSTFIX_CHECKED);
        methodBuilder.addModifiers(Modifier.PROTECTED);
        methodBuilder.addAnnotation(Override.class);

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(ClassName.get(String.class), "tag", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());

        parameterBuilder = ParameterSpec.builder(ClassName.BOOLEAN, "checked", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());

        final List<EventHandlerMethod> eventHandlerMethods = this.eventHandlerMethods.get(EventHandlerMethod.EventType.ON_CHECKED);

        if (eventHandlerMethods != null && !eventHandlerMethods.isEmpty()) {
            boolean isFirstCondition = true;

            for (final EventHandlerMethod method : eventHandlerMethods) {
                final String tag = method.getTag().toLowerCase();

                if (isFirstCondition) {
                    isFirstCondition = false;
                    methodBuilder.beginControlFlow("if (tag.equals(\"" + tag + "\"))");
                } else {
                    methodBuilder.nextControlFlow("else if (tag.equals(\"" + tag + "\"))");
                }
                methodBuilder.addStatement("presenter." + method.getMethodName() + "(checked)");
            }
            methodBuilder.endControlFlow();
        }
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildOnClickMethod(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(PREFIX_ON + POSTFIX_CLICK);
        methodBuilder.addModifiers(Modifier.PROTECTED);
        methodBuilder.addAnnotation(Override.class);

        final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(ClassName.get(String.class), "tag", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());

        final List<EventHandlerMethod> eventHandlerMethods = this.eventHandlerMethods.get(EventHandlerMethod.EventType.ON_CLICK);

        if (eventHandlerMethods != null && !eventHandlerMethods.isEmpty()) {
            boolean isFirstCondition = true;

            for (final EventHandlerMethod method : eventHandlerMethods) {
                final String tag = method.getTag().toLowerCase();

                if (isFirstCondition) {
                    isFirstCondition = false;
                    methodBuilder.beginControlFlow("if (tag.equals(\"" + tag + "\"))");
                } else {
                    methodBuilder.nextControlFlow("else if (tag.equals(\"" + tag + "\"))");
                }
                methodBuilder.addStatement("presenter." + method.getMethodName() + "()");
            }
            methodBuilder.endControlFlow();
        }
        classBuilder.addMethod(methodBuilder.build());
    }

    private void buildOnTextChangedMethod(final TypeSpec.Builder classBuilder) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(PREFIX_ON + POSTFIX_TEXT_CHANGED);
        methodBuilder.addModifiers(Modifier.PROTECTED);
        methodBuilder.addAnnotation(Override.class);

        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(ClassName.get(String.class), "tag", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());

        parameterBuilder = ParameterSpec.builder(ClassName.get(String.class), "text", Modifier.FINAL);
        methodBuilder.addParameter(parameterBuilder.build());

        final List<EventHandlerMethod> eventHandlerMethods = this.eventHandlerMethods.get(EventHandlerMethod.EventType.ON_TEXT_CHANGED);

        if (eventHandlerMethods != null && !eventHandlerMethods.isEmpty()) {
            boolean isFirstCondition = true;

            for (final EventHandlerMethod method : eventHandlerMethods) {
                final String tag = method.getTag().toLowerCase();

                if (isFirstCondition) {
                    isFirstCondition = false;
                    methodBuilder.beginControlFlow("if (tag.equals(\"" + tag + "\"))");
                } else {
                    methodBuilder.nextControlFlow("else if (tag.equals(\"" + tag + "\"))");
                }
                methodBuilder.addStatement("presenter." + method.getMethodName() + "(text)");
            }
            methodBuilder.endControlFlow();
        }
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
