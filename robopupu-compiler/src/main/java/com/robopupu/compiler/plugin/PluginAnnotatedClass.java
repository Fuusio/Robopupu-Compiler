package com.robopupu.compiler.plugin;

import com.robopupu.api.dependency.D;
import com.robopupu.api.dependency.DependencyScope;
import com.robopupu.api.plugin.Plugger;
import com.robopupu.api.plugin.Plugin;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

/**
 * {@link PluginAnnotatedClass} is a model class used for storing information about a class
 * annotated with {@link Plugin}. The class is also used for generate Java code from
 * the annotated classes.
 */
public class PluginAnnotatedClass {

    private static final String PREFIX_PLUG = "plug_";
    private static final String SUFFIX_PLUG_INVOKER = "_PlugInvoker";
    private static final String SUFFIX_PLUGGER = "_Plugger";
    private static final ClassName CLASS_NAME_PLUGGER = ClassName.get(Plugger.class);
    private static final ClassName CLASS_NAME_PLUGIN_BUS = ClassName.get("com.robopupu.api.plugin", "PluginBus");
    private static final ClassName CLASS_NAME_PLUG_INVOKER = ClassName.get("com.robopupu.api.plugin", "PlugInvoker");

    private final String annotatedClassName;
    private final HashMap<String, PlugAnnotatedField> plugFields;
    private final HashMap<String, PlugInterfaceAnnotatedInterface> plugInterfaces;
    private final TypeElement typeElement;

    public PluginAnnotatedClass(final TypeElement typeElement) throws com.robopupu.compiler.util.ProcessorException {
        this.typeElement = typeElement;
        annotatedClassName = typeElement.getSimpleName().toString();
        plugFields = new HashMap<>();
        plugInterfaces = new HashMap<>();
    }

    public void addPlugField(final String fieldName, final TypeMirror fieldType, final String scopeClass) {
        final PlugAnnotatedField field = new PlugAnnotatedField(fieldType, scopeClass);
        plugFields.put(fieldName, field);
    }

    public void addPlugInterface(final String interfaceName, final PlugInterfaceAnnotatedInterface annotatedInterface) {
        plugInterfaces.put(interfaceName, annotatedInterface);
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public void generateCode(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {
        generatePlugger(environment, elementUtils, filer);
    }

    private void generatePlugger(final ProcessingEnvironment environment, final Elements elementUtils, final Filer filer) throws IOException {

        final PackageElement packageElement = elementUtils.getPackageOf(typeElement);
        final String packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
        final String suffixedClassName = annotatedClassName + SUFFIX_PLUGGER;
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(suffixedClassName);
        classBuilder.addSuperinterface(CLASS_NAME_PLUGGER);
        classBuilder.addModifiers(Modifier.PUBLIC);

        classBuilder.addMethod(buildPlugMethod());
        classBuilder.addMethod(buildUnplugMethod());

        final TypeSpec typeSpec = classBuilder.build();
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

    private MethodSpec buildPlugMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("plug");
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.addAnnotation(Override.class);

        final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(SuppressWarnings.class);
        annotationBuilder.addMember("value", "\"unchecked\"");

        methodBuilder.addAnnotation(annotationBuilder.build());
        methodBuilder.addParameter(ClassName.OBJECT, "plugin", Modifier.FINAL);
        methodBuilder.addParameter(CLASS_NAME_PLUGIN_BUS, "bus", Modifier.FINAL);
        methodBuilder.addParameter(TypeName.BOOLEAN, "useHandler", Modifier.FINAL);

        com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

        writer.k(com.robopupu.compiler.util.Keyword.FINAL).a(annotatedClassName);
        writer.a(" typedPlugin = (").a(annotatedClassName).a(") plugin");
        methodBuilder.addStatement(writer.getCode());

        buildPlugFieldSetStatements(methodBuilder);
        buildPlugAddStatements(methodBuilder);

        return methodBuilder.build();
    }

    private void buildPlugFieldSetStatements(final MethodSpec.Builder methodBuilder) {

        for (final String fieldName : plugFields.keySet()) {
            final PlugAnnotatedField field = plugFields.get(fieldName);
            final String localPlugFieldName = PREFIX_PLUG + fieldName;
            final String interfaceSimpleName = field.getFieldType().toString();

            methodBuilder.addCode("\n");

            com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

            writer.a(interfaceSimpleName).a("_PlugInvoker ").a(localPlugFieldName).a(" = bus.getPlugInvoker(");
            writer.a(interfaceSimpleName).a(".class)");
            methodBuilder.addStatement(writer.getCode());
            methodBuilder.addCode("\n");

            methodBuilder.beginControlFlow("if (" + localPlugFieldName + " == null)");

            writer.clear();
            writer.a(localPlugFieldName).a(" = new ").a(interfaceSimpleName).a("_PlugInvoker()");
            methodBuilder.addStatement(writer.getCode());

            writer.clear();
            writer.a("bus.addPlugInvoker(").a(interfaceSimpleName).a(".class, ").a(localPlugFieldName).a(")");
            methodBuilder.addStatement(writer.getCode());

            methodBuilder.endControlFlow();

            writer.clear();

            writer.a("typedPlugin.").a(fieldName).a(" = ").a(localPlugFieldName);
            methodBuilder.addStatement(writer.getCode());

            final String scopeClass = field.getScopeClass();

            if (scopeClass != null) {
                writer.clear();
                writer.k(com.robopupu.compiler.util.Keyword.FINAL).a("$T").a(" scope_").a(fieldName).a(" = D.getScope(");
                writer.a(scopeClass).a(".class)");
                methodBuilder.addStatement(writer.getCode(), DependencyScope.class);

                writer.clear();
                writer.a("final Object instance_").a(fieldName).a(" = $T.get(scope_");
                writer.a(fieldName).a(", ").a(interfaceSimpleName).a(".class)");
                methodBuilder.addStatement(writer.getCode(), D.class);

                writer.clear();
                writer.a("PluginBus.plug(instance_").a(fieldName).a(")");
                methodBuilder.addStatement(writer.getCode());
            }
        }
    }

    private void buildPlugAddStatements(final MethodSpec.Builder methodBuilder) {
        for (final String interfaceName : plugInterfaces.keySet()) {

            methodBuilder.addCode("\n");

            final PlugInterfaceAnnotatedInterface annotatedInterface = plugInterfaces.get(interfaceName);
            final TypeElement pluginInterface = annotatedInterface.getTypeElement();
            final String interfaceSimpleName = pluginInterface.getSimpleName().toString();
            final String interfaceQualifiedSimpleName = pluginInterface.getQualifiedName().toString();
            final String fieldName = PREFIX_PLUG + interfaceSimpleName;

            com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

            writer.a("final $T<?> ").a(fieldName).a(" = bus.hasPlugInvoker(").a(interfaceQualifiedSimpleName).a(".class) ? null : new ");
            writer.a(interfaceQualifiedSimpleName).a("_PlugInvoker()");
            methodBuilder.addStatement(writer.getCode(), CLASS_NAME_PLUG_INVOKER);

            writer.clear();

            writer.a("bus.plug(typedPlugin, ").a(interfaceQualifiedSimpleName).a(".class, ").a(fieldName);

            if (annotatedInterface.getPlugMode().isBroadcast()) {
                writer.a(", useHandler ? new ").a(interfaceQualifiedSimpleName).a("_HandlerInvoker() : null)");
            } else {
                writer.a(", null)");
            }
            methodBuilder.addStatement(writer.getCode());
        }
    }

    private MethodSpec buildUnplugMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("unplug");
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.addAnnotation(Override.class);

        final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(SuppressWarnings.class);
        annotationBuilder.addMember("value", "\"unchecked\"");

        methodBuilder.addAnnotation(annotationBuilder.build());
        methodBuilder.addParameter(ClassName.OBJECT, "plugin", Modifier.FINAL);
        methodBuilder.addParameter(CLASS_NAME_PLUGIN_BUS, "bus", Modifier.FINAL);

        com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

        writer.k(com.robopupu.compiler.util.Keyword.FINAL).a(annotatedClassName);
        writer.a(" typedPlugin = (").a(annotatedClassName).a(") plugin");
        methodBuilder.addStatement(writer.getCode());
        methodBuilder.addCode("\n");

        buildPlugFieldResetStatements(methodBuilder);
        buildPlugRemoveStatements(methodBuilder);

        return methodBuilder.build();
    }

    private void buildPlugFieldResetStatements(final MethodSpec.Builder methodBuilder) {
        for (final String fieldName : plugFields.keySet()) {
            com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();
            writer.a("typedPlugin.").a(fieldName).a(" = null");
            methodBuilder.addStatement(writer.getCode());
            methodBuilder.addCode("\n");
        }
    }

    private void buildPlugRemoveStatements(final MethodSpec.Builder methodBuilder) {

        for (final String interfaceName : plugInterfaces.keySet()) {

            final PlugInterfaceAnnotatedInterface annotatedInterface = plugInterfaces.get(interfaceName);
            final TypeElement pluginInterface = annotatedInterface.getTypeElement();
            final String interfaceSimpleName = pluginInterface.getSimpleName().toString();
            final String interfaceQualifiedSimpleName = pluginInterface.getQualifiedName().toString();
            com.robopupu.compiler.util.JavaWriter writer = new com.robopupu.compiler.util.JavaWriter();

            final String fieldName = PREFIX_PLUG + interfaceSimpleName;

            writer.a("final ").a(interfaceName).a(SUFFIX_PLUG_INVOKER).s().a(fieldName);
            writer.a(" = bus.getPlugInvoker(").a(interfaceQualifiedSimpleName).a(".class)");
            methodBuilder.addStatement(writer.getCode());

            writer.clear();
            writer.a(fieldName).a(".removePlugin(typedPlugin)");
            methodBuilder.addStatement(writer.getCode());
            methodBuilder.addCode("\n");
        }
    }
}
