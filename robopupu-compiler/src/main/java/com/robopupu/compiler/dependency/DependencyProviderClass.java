package com.robopupu.compiler.dependency;

import com.robopupu.api.dependency.DependencyQuery;
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
    private static final ClassName CLASS_DEPENDENCY_QUERY = ClassName.get(DependencyQuery.class);

    private final String className;
    private final TypeElement classElement;
    private final ArrayList<ProviderClass> providerClasses;
    private final ArrayList<ProviderConstructor> providerConstructors;
    private final ArrayList<ProviderMethod> providerMethods;

    public DependencyProviderClass(final TypeElement classElement) throws ProcessorException {
        this.classElement = classElement;
        className = classElement.getSimpleName().toString();
        providerClasses = new ArrayList<>();
        providerConstructors = new ArrayList<>();
        providerMethods = new ArrayList<>();
    }

    public void addProviderClass(final ProviderClass providerClass) {
        providerClasses.add(providerClass);
    }

    public void addProviderConstructor(final ProviderConstructor providerConstructor) {
        providerConstructors.add(providerConstructor);
    }

    public void addProviderMethod(final ProviderMethod providerMethod) {
        providerMethods.add(providerMethod);
    }

    /**
     * Generates code for {@link DependencyProvider} implementation.
     * @param elementUtils An {@link Elements} providing type utilities.
     * @param filer A {@link Filer} to write generated code to a file.
     * @throws IOException
     */
    public void generateCode(final Elements elementUtils, final Filer filer) throws IOException {

        // Check if there is need to generate code for a DependencyProvider implementation

        if (providerClasses.isEmpty() && providerConstructors.isEmpty() && providerMethods.isEmpty()) {
            return;
        }

        // Generate code for a DependencyProvider implementation

        final PackageElement packageElement = elementUtils.getPackageOf(classElement);
        final String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
        final String suffixedClassName = className + SUFFIX_DEPENDENCY_PROVIDER;
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

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getDependencies");
        methodBuilder.addModifiers(Modifier.PROTECTED);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.addAnnotation(annotationBuilder.build());

        final ParameterizedTypeName parameterizedType = ParameterizedTypeName.get(CLASS_DEPENDENCY_QUERY, TypeVariableName.get("T"));
        methodBuilder.addParameter(parameterizedType, "query", Modifier.FINAL);
        methodBuilder.returns(TypeVariableName.get("<T> void"));

        for (final ProviderClass providerClass : providerClasses) {

            final String providedType = providerClass.getProvidedType();
            final String implementationType = providerClass.getType();

            methodBuilder.beginControlFlow(String.format("if (query.matches(%1$s.class, %2$s.class))", providedType, implementationType));

            final JavaWriter writer = new JavaWriter();

            writer.a("if (query.add((T) new ");

            final String providerType = providerClass.getTypeElement().toString();

            writer.a(providerType).a("()))");

            methodBuilder.beginControlFlow(writer.getCode());
            methodBuilder.addStatement(Keyword.RETURN.toString());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }

        for (final ProviderMethod providerMethod : providerMethods) {

            final String providedType = providerMethod.getProvidedType();
            final String implementationType = providerMethod.getReturnType(); // XXX

            methodBuilder.beginControlFlow(String.format("if (query.matches(%1$s.class, %2$s.class))", providedType, implementationType));

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

                writer.c().a("if (query.add((T) ((");
                writer.a(providerMethod.getDependencyScopeType());
                writer.a(")scope).").a(providerMethod.getMethodName()).a("(");

                int index = 0;

                for (final VariableElement parameter : parameters) {

                    if (index++ > 0) {
                        writer.a(", ");
                    }
                    writer.a(parameter.getSimpleName().toString());
                }
                writer.a(")))");
            } else {
                writer.c().a("if (query.add((T) ((");

                writer.a(providerMethod.getDependencyScopeType());
                writer.a(")scope).").a(providerMethod.getMethodName()).a("()))");
            }

            methodBuilder.beginControlFlow(writer.getCode());
            methodBuilder.addStatement(Keyword.RETURN.toString());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }

        for (final ProviderConstructor providerConstructor : providerConstructors) {

            final String providedType = providerConstructor.getProvidedType();
            final String implementationType = providerConstructor.getType();

            methodBuilder.beginControlFlow(String.format("if (query.matches(%1$s.class, %2$s.class))", providedType, implementationType));

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

                writer.c().a("if (query.add((T) new ");
                writer.a(providerConstructor.getConstructorName()).a("(");

                int index = 0;

                for (final VariableElement parameter : parameters) {

                    if (index++ > 0) {
                        writer.a(", ");
                    }
                    writer.a(parameter.getSimpleName().toString());
                }
                writer.a(")))");
            } else {
                writer.c().a("if (query.add((T) new ");
                writer.a(providerConstructor.getConstructorName()).a("()))");
            }

            methodBuilder.beginControlFlow(writer.getCode());
            methodBuilder.addStatement(Keyword.RETURN.toString());
            methodBuilder.endControlFlow();
            methodBuilder.endControlFlow();
        }
        return methodBuilder.build();
    }

    public String getPackageName(final Elements elementUtils) {
        final PackageElement packageElement = elementUtils.getPackageOf(classElement);
        return packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
    }
}
