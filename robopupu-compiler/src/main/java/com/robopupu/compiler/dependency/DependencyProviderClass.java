package com.robopupu.compiler.dependency;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import com.robopupu.api.dependency.D;
import com.robopupu.api.dependency.DependencyProvider;
import com.robopupu.compiler.util.JavaWriter;
import com.robopupu.compiler.util.Keyword;
import com.robopupu.compiler.util.ProcessorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * {@link DependencyProviderClass} is a model class used for storing information about TODO
 */
public class DependencyProviderClass {

    private static final String SUFFIX_DEPENDENCY_PROVIDER = "_DependencyProvider";
    private static final ClassName CLASS_DEPENDENCY_PROVIDER = ClassName.get(DependencyProvider.class);

    private final String mClassName;
    private final TypeElement mClassElement;
    private final ArrayList<ProviderClass> mProviderClasses;
    private final ArrayList<ProviderConstructor> mProviderConstructors;
    private final ArrayList<ProviderMethod> mProviderMethods;

    public DependencyProviderClass(final TypeElement classElement) throws ProcessorException {
        mClassElement = classElement;
        mClassName = classElement.getSimpleName().toString();
        mProviderClasses = new ArrayList<>();
        mProviderConstructors = new ArrayList<>();
        mProviderMethods = new ArrayList<>();
    }

    public void addProviderClass(final ProviderClass providerClass) {
        mProviderClasses.add(providerClass);
    }

    public void addProviderConstructor(final ProviderConstructor providerConstructor) {
        mProviderConstructors.add(providerConstructor);
    }

    public void addProviderMethod(final ProviderMethod providerMethod) {
        mProviderMethods.add(providerMethod);
    }

    public void generateCode(final Elements elementUtils, final Filer filer) throws IOException {
        final PackageElement packageElement = elementUtils.getPackageOf(mClassElement);
        final String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
        final String suffixedClassName = mClassName + SUFFIX_DEPENDENCY_PROVIDER;
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);

        classBuilder.superclass(CLASS_DEPENDENCY_PROVIDER);
        classBuilder.addModifiers(Modifier.PUBLIC);
        classBuilder.addMethod(buildGetDependencyMethod());

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec buildGetDependencyMethod() {

        final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(SuppressWarnings.class);
        annotationBuilder.addMember("value", "\"unchecked\"");

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getDependency");
        methodBuilder.addModifiers(Modifier.PROTECTED);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.addAnnotation(annotationBuilder.build());

        final ParameterizedTypeName parameterizedType = ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T"));
        methodBuilder.addParameter(parameterizedType, "dependencyType", Modifier.FINAL);
        methodBuilder.returns(TypeVariableName.get("<T> T"));

        boolean isFirstCondition = true;

        for (final ProviderClass providerClass : mProviderClasses) {

            final String returnType = providerClass.getProvidedType();

            if (isFirstCondition) {
                isFirstCondition = false;
                methodBuilder.beginControlFlow(String.format("if (dependencyType.isAssignableFrom(%s.class))", returnType));
            } else {
                methodBuilder.nextControlFlow(String.format("else if (dependencyType.isAssignableFrom(%s.class))", returnType));
            }

            final JavaWriter writer = new JavaWriter();

            writer.k(com.robopupu.compiler.util.Keyword.RETURN).a("(T) new ");

            final String providerType = providerClass.getTypeElement().toString();

            writer.a(providerType).a("()");
            methodBuilder.addStatement(writer.getCode());
        }

        for (final ProviderMethod providerMethod : mProviderMethods) {

            final String returnType = providerMethod.getProvidedType();

            if (isFirstCondition) {
                isFirstCondition = false;
                methodBuilder.beginControlFlow(String.format("if (dependencyType.isAssignableFrom(%s.class))", returnType));
            } else {
                methodBuilder.nextControlFlow(String.format("else if (dependencyType.isAssignableFrom(%s.class))", returnType));
            }

            final JavaWriter writer = new JavaWriter();

            if (providerMethod.hasParameters()) {
                final List<? extends VariableElement> parameters = providerMethod.getParameters();

                for (final VariableElement parameter : parameters) {
                    final String parameterType = parameter.asType().toString();
                    final String parameterName = parameter.getSimpleName().toString();
                    writer.c().k(Keyword.FINAL).append(parameterType).s().a(parameterName);
                    writer.a(" = $T.get(").a(parameterType).a(".class)");
                    methodBuilder.addStatement(writer.getCode(), D.class);
                }

                writer.c().k(com.robopupu.compiler.util.Keyword.RETURN).a("(T) ((").a(providerMethod.getDependencyScopeType());
                writer.a(")mScope).").a(providerMethod.getMethodName()).a("(");

                int index = 0;

                for (final VariableElement parameter : parameters) {

                    if (index++ > 0) {
                        writer.a(", ");
                    }
                    writer.a(parameter.getSimpleName().toString());
                }
                writer.a(")");
            } else {
                writer.k(Keyword.RETURN).a("(T) ((").a(providerMethod.getDependencyScopeType());
                writer.a(")mScope).").a(providerMethod.getMethodName()).a("()");
            }
            methodBuilder.addStatement(writer.getCode());
        }

        for (final ProviderConstructor providerConstructor : mProviderConstructors) {

            final String returnType = providerConstructor.getProvidedType();

            if (isFirstCondition) {
                isFirstCondition = false;
                methodBuilder.beginControlFlow(String.format("if (dependencyType.isAssignableFrom(%s.class))", returnType));
            } else {
                methodBuilder.nextControlFlow(String.format("else if (dependencyType.isAssignableFrom(%s.class))", returnType));
            }

            final JavaWriter writer = new JavaWriter();

            if (providerConstructor.hasParameters()) {
                final List<? extends VariableElement> parameters = providerConstructor.getParameters();

                for (final VariableElement parameter : parameters) {
                    final String parameterType = parameter.asType().toString();
                    final String parameterName = parameter.getSimpleName().toString();
                    writer.c().k(com.robopupu.compiler.util.Keyword.FINAL).append(parameterType).s().a(parameterName);
                    writer.a(" = $T.get(").a(parameterType).a(".class)");
                    methodBuilder.addStatement(writer.getCode(), D.class);
                }

                writer.c().k(com.robopupu.compiler.util.Keyword.RETURN).a("(T) new ");
                writer.a(providerConstructor.getConstructorName()).a("(");

                int index = 0;

                for (final VariableElement parameter : parameters) {

                    if (index++ > 0) {
                        writer.a(", ");
                    }
                    writer.a(parameter.getSimpleName().toString());
                }
                writer.a(")");
            } else {
                writer.k(com.robopupu.compiler.util.Keyword.RETURN).a("(T) new ");
                writer.a(providerConstructor.getConstructorName()).a("()");
            }
            methodBuilder.addStatement(writer.getCode());
        }

        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return null");
        return methodBuilder.build();
    }

    public String getPackageName(final Elements elementUtils) {
        final PackageElement packageElement = elementUtils.getPackageOf(mClassElement);
        return packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
    }
}
